package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiTeamAutoCreationService {

    // 자동 팀 생성 시 기본으로 맞추려는 팀당 인원 수입니다.
    private static final int TEAM_SIZE = 5;

    // 설문을 완료한 학생 계정과 관리자 계정을 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 자동 생성된 팀을 저장하고 현재 팀 존재 여부를 확인하는 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 팀에 학생을 배정하거나 현재 팀원 목록을 조회하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 팀 생성 후 팀별 채팅방을 자동 생성하는 Repository 필드입니다.
    private final ChatRoomRepository chatRoomRepository;

    // 팀 채팅방의 기본 채널을 자동 생성하는 Repository 필드입니다.
    private final ChatChannelRepository chatChannelRepository;

    // 대량 삭제와 user_analysis upsert를 직접 SQL로 처리하기 위한 필드입니다.
    private final JdbcTemplate jdbcTemplate;

    // 학생 프로필 기반으로 기존 팀 데이터를 초기화하고 새 팀/팀원/채팅방을 자동 생성하는 기능입니다.
    @Transactional
    public AiTeamSummaryResponseDto recreateTeamsByStudentProfiles() {
        List<StudentProfile> profiles = loadStudentProfiles();
        analyzeSkillLevels(profiles);
        saveAnalysisResults(profiles);
        resetTeamData();

        User channelCreator = findChannelCreator();
        List<List<StudentProfile>> matchedTeams = matchTeams(profiles);
        List<TeamResult> teamResults = persistTeams(matchedTeams, channelCreator);

        return buildSummary(teamResults, profiles.size());
    }

    // 현재 DB에 저장된 팀 정보를 기준으로 AI 팀 요약 응답을 다시 만드는 기능입니다.
    @Transactional(readOnly = true)
    public AiTeamSummaryResponseDto summarizeCurrentTeams() {
        List<TeamResult> teamResults = teamRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Team::getId))
                .map(team -> {
                    List<TeamUser> teamUsers = teamUserRepository.findByTeamId(team.getId());
                    List<StudentProfile> members = teamUsers.stream()
                            .map(this::toCurrentProfile)
                            .toList();
                    StudentProfile leader = teamUsers.stream()
                            .filter(teamUser -> teamUser.getLeaderRole() == LeaderRole.LEADER)
                            .findFirst()
                            .map(this::toCurrentProfile)
                            .orElseGet(() -> chooseLeader(members));
                    return new TeamResult(team, members, leader);
                })
                .toList();

        return buildSummary(teamResults, (int) userRepository.countByAccountRole(AccountRole.STUDENT));
    }

    // 이미 팀이 생성되어 있는지 확인하는 기능입니다.
    public boolean hasCreatedTeams() {
        return teamRepository.count() > 0;
    }

    // 학생 계정 목록을 설문 정보 기반 StudentProfile 목록으로 변환하는 기능입니다.
    private List<StudentProfile> loadStudentProfiles() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getAccountRole() == AccountRole.STUDENT)
                .sorted(Comparator.comparing(User::getUserId))
                .map(user -> {
                    StudentRole role = user.getStudentRole() != null ? user.getStudentRole() : StudentRole.BACKEND;
                    List<String> skills = safeList(user.getSkill());
                    List<String> experiences = safeList(user.getExperience());
                    double score = calculateScore(user, skills, experiences);

                    return StudentProfile.builder()
                            .user(user)
                            .role(role)
                            .roleGroup(toRoleGroup(role))
                            .skills(skills)
                            .experiences(experiences)
                            .score(score)
                            .build();
                })
                .toList();
    }

    // null 목록을 빈 목록으로 바꾸고 null 원소를 제거하는 기능입니다.
    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    // 기술 스택, 구현 경험, 팀장 희망, 선호 팀원 수를 기준으로 매칭 점수를 계산하는 기능입니다.
    private double calculateScore(User user, List<String> skills, List<String> experiences) {
        double score = 10;
        score += Math.min(skills.size(), 6) * 5;
        score += Math.min(experiences.size(), 5) * 4;
        score += user.isWantsLeader() ? 4 : 0;
        score += safeList(user.getPreferredTeammates()).size();
        return score;
    }

    // StudentRole enum을 AI 요약에서 사용하는 역할군 문자열로 변환하는 기능입니다.
    private String toRoleGroup(StudentRole role) {
        return switch (role) {
            case BACKEND -> "backend";
            case FRONTEND -> "frontend";
            case AI -> "ai_data";
            case APP -> "app";
            case DESIGN -> "design";
        };
    }

    // 매칭 점수를 기준으로 상위 20%, 중간, 하위 20% 실력 등급을 부여하는 기능입니다.
    private void analyzeSkillLevels(List<StudentProfile> profiles) {
        List<StudentProfile> sorted = profiles.stream()
                .sorted(Comparator
                        .comparingDouble(StudentProfile::getScore)
                        .reversed()
                        .thenComparing(profile -> profile.getUser().getUserId()))
                .toList();

        int upperCount = (int) Math.ceil(sorted.size() * 0.2);
        int lowerStart = Math.max(sorted.size() - upperCount, upperCount);

        for (int index = 0; index < sorted.size(); index++) {
            StudentLevel level;
            if (index < upperCount) {
                level = StudentLevel.UPPER;
            } else if (index >= lowerStart) {
                level = StudentLevel.LOWER;
            } else {
                level = StudentLevel.MIDDLE;
            }

            sorted.get(index).setLevel(level);
        }
    }

    // 계산된 학생 분석 결과와 실력 등급을 user_analysis 테이블에 저장하거나 갱신하는 기능입니다.
    private void saveAnalysisResults(List<StudentProfile> profiles) {
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO user_analysis (user_id, created_at, updated_at, analysis_result, student_level)
                VALUES (?, NOW(6), NOW(6), ?, ?)
                ON DUPLICATE KEY UPDATE
                    updated_at = NOW(6),
                    analysis_result = VALUES(analysis_result),
                    student_level = VALUES(student_level)
                """,
                profiles,
                100,
                (ps, profile) -> {
                    ps.setString(1, profile.getUser().getUserId());
                    ps.setString(2, buildAnalysisText(profile));
                    ps.setString(3, profile.getLevel().name());
                }
        );
    }

    // 현재 DB의 TeamUser 정보를 StudentProfile로 변환해 요약 생성에 재사용하는 기능입니다.
    private StudentProfile toCurrentProfile(TeamUser teamUser) {
        User user = teamUser.getUser();
        List<String> skills = safeList(user.getSkill());
        List<String> experiences = safeList(user.getExperience());

        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .role(teamUser.getStudentRole())
                .roleGroup(toRoleGroup(teamUser.getStudentRole()))
                .skills(skills)
                .experiences(experiences)
                .score(calculateScore(user, skills, experiences))
                .build();
        profile.setLevel(loadLevel(user.getUserId()));
        return profile;
    }

    // user_analysis 테이블에서 학생의 실력 등급을 조회하고 없으면 MIDDLE로 처리하는 기능입니다.
    private StudentLevel loadLevel(String userId) {
        List<StudentLevel> levels = jdbcTemplate.query(
                "SELECT student_level FROM user_analysis WHERE user_id = ?",
                (rs, rowNum) -> StudentLevel.valueOf(rs.getString("student_level")),
                userId
        );
        return levels.isEmpty() ? StudentLevel.MIDDLE : levels.get(0);
    }

    // 학생 역할/스킬/경험/등급을 사람이 읽을 수 있는 분석 문장으로 만드는 기능입니다.
    private String buildAnalysisText(StudentProfile profile) {
        return "%s 역할 희망, 스킬 %d개, 경험 %d개를 기준으로 %s 등급으로 분석되었습니다."
                .formatted(
                        profile.getRole().name(),
                        profile.getSkills().size(),
                        profile.getExperiences().size(),
                        toKoreanLevel(profile.getLevel())
                );
    }

    // 팀 재생성 전에 기존 팀/채팅/일지 관련 데이터를 삭제하는 기능입니다.
    private void resetTeamData() {
        jdbcTemplate.update("DELETE FROM chat_read_status");
        jdbcTemplate.update("DELETE FROM chat_messages");
        jdbcTemplate.update("DELETE FROM chat_channels");
        jdbcTemplate.update("DELETE FROM chat_rooms");
        jdbcTemplate.update("DELETE FROM journal_entries");
        jdbcTemplate.update("DELETE FROM journals");
        jdbcTemplate.update("DELETE FROM team_projects");
        jdbcTemplate.update("DELETE FROM team_members");
        jdbcTemplate.update("DELETE FROM teams");
    }

    // 자동 생성 채팅 채널의 createdBy에 넣을 관리자 계정을 찾는 기능입니다.
    private User findChannelCreator() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getAccountRole() == AccountRole.ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("채팅 채널 생성자로 사용할 관리자 계정이 없습니다."));
    }

    // 역할별 큐를 만들어 각 팀에 역할이 최대한 분산되도록 학생을 배치하는 기능입니다.
    private List<List<StudentProfile>> matchTeams(List<StudentProfile> profiles) {
        int teamCount = (int) Math.ceil((double) profiles.size() / TEAM_SIZE);
        List<List<StudentProfile>> teams = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) {
            teams.add(new ArrayList<>());
        }

        Map<StudentRole, Queue<StudentProfile>> roleQueues = new EnumMap<>(StudentRole.class);
        profiles.stream()
                .collect(Collectors.groupingBy(StudentProfile::getRole, () -> new EnumMap<>(StudentRole.class), Collectors.toList()))
                .forEach((role, roleProfiles) -> roleQueues.put(
                        role,
                        new ArrayDeque<>(roleProfiles.stream()
                                .sorted(Comparator
                                        .comparingDouble(StudentProfile::getScore)
                                        .reversed()
                                        .thenComparing(profile -> profile.getUser().getUserId()))
                                .toList())
                ));

        List<StudentRole> roleOrder = List.of(
                StudentRole.BACKEND,
                StudentRole.FRONTEND,
                StudentRole.AI,
                StudentRole.APP,
                StudentRole.DESIGN
        );

        for (StudentRole role : roleOrder) {
            Queue<StudentProfile> queue = roleQueues.getOrDefault(role, new ArrayDeque<>());
            int teamIndex = 0;
            while (!queue.isEmpty()) {
                teams.get(teamIndex % teamCount).add(queue.poll());
                teamIndex++;
            }
        }

        return teams;
    }

    // 매칭 결과를 Team, TeamUser, ChatRoom, ChatChannel 테이블에 저장하는 기능입니다.
    private List<TeamResult> persistTeams(List<List<StudentProfile>> matchedTeams, User channelCreator) {
        List<TeamResult> results = new ArrayList<>();
        int teamNumber = 1;

        for (List<StudentProfile> members : matchedTeams) {
            Team team = teamRepository.save(Team.builder()
                    .teamName("2학년 " + teamNumber + "팀")
                    .grade(Grade.GRADE_2)
                    .status(TeamStatus.APPROVED)
                    .build());

            StudentProfile leader = chooseLeader(members);
            for (StudentProfile member : members) {
                teamUserRepository.save(TeamUser.builder()
                        .team(team)
                        .user(member.getUser())
                        .studentRole(member.getRole())
                        .leaderRole(member == leader ? LeaderRole.LEADER : LeaderRole.MEMBER)
                        .build());
            }

            ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                    .team(team)
                    .build());
            chatChannelRepository.save(ChatChannel.builder()
                    .chatRoom(room)
                    .channelName("공통")
                    .createdBy(channelCreator)
                    .build());

            results.add(new TeamResult(team, members, leader));
            teamNumber++;
        }

        return results;
    }

    // 팀장 희망자 중 점수가 높은 학생을 우선 팀장으로 선택하는 기능입니다.
    private StudentProfile chooseLeader(List<StudentProfile> members) {
        return members.stream()
                .filter(member -> member.getUser().isWantsLeader())
                .max(Comparator.comparingDouble(StudentProfile::getScore))
                .orElseGet(() -> members.stream()
                        .max(Comparator.comparingDouble(StudentProfile::getScore))
                        .orElseThrow());
    }

    // 팀 생성 결과 목록을 AI 팀 요약 최상위 응답 DTO로 변환하는 기능입니다.
    private AiTeamSummaryResponseDto buildSummary(List<TeamResult> teamResults, int totalStudents) {
        AiTeamSummaryResponseDto response = new AiTeamSummaryResponseDto();
        response.setTotalStudents(totalStudents);
        response.setTotalTeams(teamResults.size());
        response.setTeams(teamResults.stream().map(this::buildTeamDto).toList());
        return response;
    }

    // 한 팀의 매칭 결과를 AI 팀 요약의 팀 DTO로 변환하는 기능입니다.
    private AiTeamSummaryResponseDto.TeamDto buildTeamDto(TeamResult result) {
        AiTeamSummaryResponseDto.TeamDto dto = new AiTeamSummaryResponseDto.TeamDto();
        dto.setTeamName(result.team().getTeamName());
        dto.setTotalPeople(result.members().size());
        dto.setLeader(result.leader().getUser().getName());
        dto.setRoleCounts(buildRoleCounts(result.members()));
        dto.setMembers(result.members().stream().map(this::buildMemberDto).toList());
        dto.setTopStackScores(buildTeamTopStacks(result.members()));
        dto.setSkillLevelCounts(buildSkillLevelCounts(result.members()));
        dto.setMatchingReason(buildMatchingReason(result));
        dto.setStrengths(buildTeamStrengths(result.members()));
        dto.setWeaknesses(buildTeamWeaknesses(result.members()));
        return dto;
    }

    // 팀원 목록에서 역할군별 인원 수 DTO 목록을 만드는 기능입니다.
    private List<AiTeamSummaryResponseDto.RoleCountDto> buildRoleCounts(List<StudentProfile> members) {
        Map<String, Long> counts = members.stream()
                .collect(Collectors.groupingBy(StudentProfile::getRoleGroup, LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .map(entry -> {
                    AiTeamSummaryResponseDto.RoleCountDto dto = new AiTeamSummaryResponseDto.RoleCountDto();
                    dto.setRoleGroup(entry.getKey());
                    dto.setCount(entry.getValue().intValue());
                    return dto;
                })
                .toList();
    }

    // 학생 프로필 하나를 AI 팀 요약의 팀원 DTO로 변환하는 기능입니다.
    private AiTeamSummaryResponseDto.MemberDto buildMemberDto(StudentProfile profile) {
        AiTeamSummaryResponseDto.MemberDto dto = new AiTeamSummaryResponseDto.MemberDto();
        dto.setName(profile.getUser().getName());
        dto.setRole(profile.getRole().name().toLowerCase());
        dto.setRoleGroup(profile.getRoleGroup());
        dto.setSkillLevel(toKoreanLevel(profile.getLevel()));
        dto.setScore(profile.getScore());
        dto.setStrength(buildStrength(profile));
        dto.setTopStackScores(buildMemberTopStacks(profile));
        return dto;
    }

    // 학생의 기술 스택과 경험을 조합해 강점 문장을 만드는 기능입니다.
    private String buildStrength(StudentProfile profile) {
        String skillText = profile.getSkills().isEmpty() ? "등록된 스킬 없음" : String.join(", ", profile.getSkills());
        String experienceText = profile.getExperiences().isEmpty() ? "등록된 경험 없음" : String.join(", ", profile.getExperiences());
        return skillText + " 기반으로 " + experienceText + " 경험을 보유했습니다.";
    }

    // 학생 개인의 상위 기술 스택 점수 목록을 만드는 기능입니다.
    private List<AiTeamSummaryResponseDto.MemberTopStackScoreDto> buildMemberTopStacks(StudentProfile profile) {
        return profile.getSkills().stream()
                .limit(2)
                .map(skill -> {
                    AiTeamSummaryResponseDto.MemberTopStackScoreDto dto = new AiTeamSummaryResponseDto.MemberTopStackScoreDto();
                    dto.setStack(skill);
                    dto.setScore((int) Math.round(profile.getScore() / 5));
                    return dto;
                })
                .toList();
    }

    // 팀 전체에서 대표 기술 스택 점수 목록을 만드는 기능입니다.
    private List<AiTeamSummaryResponseDto.TeamTopStackScoreDto> buildTeamTopStacks(List<StudentProfile> members) {
        return members.stream()
                .flatMap(member -> member.getSkills().stream().limit(1).map(skill -> Map.entry(member, skill)))
                .limit(2)
                .map(entry -> {
                    AiTeamSummaryResponseDto.TeamTopStackScoreDto dto = new AiTeamSummaryResponseDto.TeamTopStackScoreDto();
                    dto.setStudent(entry.getKey().getUser().getName());
                    dto.setStack(entry.getValue());
                    dto.setScore((int) Math.round(entry.getKey().getScore() / 5));
                    return dto;
                })
                .toList();
    }

    // 팀원들의 상/중/하 실력 등급 분포를 계산하는 기능입니다.
    private Map<String, Integer> buildSkillLevelCounts(List<StudentProfile> members) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("상", 0);
        counts.put("중", 0);
        counts.put("하", 0);
        for (StudentProfile member : members) {
            String level = toKoreanLevel(member.getLevel());
            counts.put(level, counts.get(level) + 1);
        }
        return counts;
    }

    // 팀 역할 분산과 팀장 선택 이유를 설명하는 매칭 이유 문장을 만드는 기능입니다.
    private String buildMatchingReason(TeamResult result) {
        String roles = result.members().stream()
                .map(member -> member.getRole().name())
                .distinct()
                .collect(Collectors.joining(", "));
        return roles + " 역할을 분산 배치하고, " + result.leader().getUser().getName()
                + "을 팀장으로 두어 실력 수준과 구현 경험이 섞이도록 자동 생성했습니다.";
    }

    // 팀의 역할군 구성을 기반으로 팀 강점 문장을 만드는 기능입니다.
    private String buildTeamStrengths(List<StudentProfile> members) {
        return members.stream()
                .map(StudentProfile::getRoleGroup)
                .distinct()
                .collect(Collectors.joining(", "))
                + " 역할군이 함께 있어 기능 구현 범위를 넓게 커버할 수 있습니다.";
    }

    // 하 등급 학생 수를 기준으로 팀 보완점 문장을 만드는 기능입니다.
    private String buildTeamWeaknesses(List<StudentProfile> members) {
        long lowerCount = members.stream().filter(member -> member.getLevel() == StudentLevel.LOWER).count();
        return lowerCount > 0
                ? "하 등급 학생 " + lowerCount + "명은 초기 구현 속도에서 보조가 필요할 수 있습니다."
                : "뚜렷한 하 등급 리스크는 낮지만 역할 간 작업 범위 조율이 필요합니다.";
    }

    // StudentLevel enum을 화면 표시용 한국어 등급으로 변환하는 기능입니다.
    private String toKoreanLevel(StudentLevel level) {
        return switch (level) {
            case UPPER -> "상";
            case MIDDLE -> "중";
            case LOWER -> "하";
        };
    }

    @lombok.Builder
    @lombok.Getter
    // 자동 팀 매칭에 필요한 학생 정보를 메모리에서 들고 다니는 내부 DTO입니다.
    private static class StudentProfile {
        // 실제 User 엔티티를 참조하는 필드입니다.
        private final User user;

        // 학생의 세부 개발 역할을 저장하는 필드입니다.
        private final StudentRole role;

        // AI 요약에서 사용할 역할군 문자열을 저장하는 필드입니다.
        private final String roleGroup;

        // 학생 기술 스택 목록을 저장하는 필드입니다.
        private final List<String> skills;

        // 학생 구현 경험 목록을 저장하는 필드입니다.
        private final List<String> experiences;

        // 자동 매칭에 사용할 학생 점수를 저장하는 필드입니다.
        private final double score;

        // 점수를 기준으로 계산된 상/중/하 등급을 저장하는 필드입니다.
        private StudentLevel level;

        // 분석된 학생 실력 등급을 설정하는 기능입니다.
        private void setLevel(StudentLevel level) {
            this.level = level;
        }
    }

    // 저장된 팀, 팀원 목록, 선택된 팀장을 함께 묶는 내부 결과 DTO입니다.
    private record TeamResult(Team team, List<StudentProfile> members, StudentProfile leader) {
    }
}
