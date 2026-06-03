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

    private static final int TEAM_SIZE = 5;

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatChannelRepository chatChannelRepository;
    private final JdbcTemplate jdbcTemplate;

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

    public boolean hasCreatedTeams() {
        return teamRepository.count() > 0;
    }

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

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }

    private double calculateScore(User user, List<String> skills, List<String> experiences) {
        double score = 10;
        score += Math.min(skills.size(), 6) * 5;
        score += Math.min(experiences.size(), 5) * 4;
        score += user.isWantsLeader() ? 4 : 0;
        score += safeList(user.getPreferredTeammates()).size();
        return score;
    }

    private String toRoleGroup(StudentRole role) {
        return switch (role) {
            case BACKEND -> "backend";
            case FRONTEND -> "frontend";
            case AI -> "ai_data";
            case APP -> "app";
            case DESIGN -> "design";
        };
    }

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

    private StudentLevel loadLevel(String userId) {
        List<StudentLevel> levels = jdbcTemplate.query(
                "SELECT student_level FROM user_analysis WHERE user_id = ?",
                (rs, rowNum) -> StudentLevel.valueOf(rs.getString("student_level")),
                userId
        );
        return levels.isEmpty() ? StudentLevel.MIDDLE : levels.get(0);
    }

    private String buildAnalysisText(StudentProfile profile) {
        return "%s 역할 희망, 스킬 %d개, 경험 %d개를 기준으로 %s 등급으로 분석되었습니다."
                .formatted(
                        profile.getRole().name(),
                        profile.getSkills().size(),
                        profile.getExperiences().size(),
                        toKoreanLevel(profile.getLevel())
                );
    }

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

    private User findChannelCreator() {
        return userRepository.findAll()
                .stream()
                .filter(user -> user.getAccountRole() == AccountRole.ADMIN)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("채팅 채널 생성자로 사용할 관리자 계정이 없습니다."));
    }

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

    private StudentProfile chooseLeader(List<StudentProfile> members) {
        return members.stream()
                .filter(member -> member.getUser().isWantsLeader())
                .max(Comparator.comparingDouble(StudentProfile::getScore))
                .orElseGet(() -> members.stream()
                        .max(Comparator.comparingDouble(StudentProfile::getScore))
                        .orElseThrow());
    }

    private AiTeamSummaryResponseDto buildSummary(List<TeamResult> teamResults, int totalStudents) {
        AiTeamSummaryResponseDto response = new AiTeamSummaryResponseDto();
        response.setTotalStudents(totalStudents);
        response.setTotalTeams(teamResults.size());
        response.setTeams(teamResults.stream().map(this::buildTeamDto).toList());
        return response;
    }

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

    private String buildStrength(StudentProfile profile) {
        String skillText = profile.getSkills().isEmpty() ? "등록된 스킬 없음" : String.join(", ", profile.getSkills());
        String experienceText = profile.getExperiences().isEmpty() ? "등록된 경험 없음" : String.join(", ", profile.getExperiences());
        return skillText + " 기반으로 " + experienceText + " 경험을 보유했습니다.";
    }

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

    private String buildMatchingReason(TeamResult result) {
        String roles = result.members().stream()
                .map(member -> member.getRole().name())
                .distinct()
                .collect(Collectors.joining(", "));
        return roles + " 역할을 분산 배치하고, " + result.leader().getUser().getName()
                + "을 팀장으로 두어 실력 수준과 구현 경험이 섞이도록 자동 생성했습니다.";
    }

    private String buildTeamStrengths(List<StudentProfile> members) {
        return members.stream()
                .map(StudentProfile::getRoleGroup)
                .distinct()
                .collect(Collectors.joining(", "))
                + " 역할군이 함께 있어 기능 구현 범위를 넓게 커버할 수 있습니다.";
    }

    private String buildTeamWeaknesses(List<StudentProfile> members) {
        long lowerCount = members.stream().filter(member -> member.getLevel() == StudentLevel.LOWER).count();
        return lowerCount > 0
                ? "하 등급 학생 " + lowerCount + "명은 초기 구현 속도에서 보조가 필요할 수 있습니다."
                : "뚜렷한 하 등급 리스크는 낮지만 역할 간 작업 범위 조율이 필요합니다.";
    }

    private String toKoreanLevel(StudentLevel level) {
        return switch (level) {
            case UPPER -> "상";
            case MIDDLE -> "중";
            case LOWER -> "하";
        };
    }

    @lombok.Builder
    @lombok.Getter
    private static class StudentProfile {
        private final User user;
        private final StudentRole role;
        private final String roleGroup;
        private final List<String> skills;
        private final List<String> experiences;
        private final double score;
        private StudentLevel level;

        private void setLevel(StudentLevel level) {
            this.level = level;
        }
    }

    private record TeamResult(Team team, List<StudentProfile> members, StudentProfile leader) {
    }
}
