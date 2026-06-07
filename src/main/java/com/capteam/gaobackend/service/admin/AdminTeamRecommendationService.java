package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.dto.team.SwapRecommendationMembersRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.*;
import com.capteam.gaobackend.exception.AiServerException;
import com.capteam.gaobackend.repository.*;
import com.capteam.gaobackend.util.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamRecommendationService {

    // AI 서버를 호출하는 클라이언트 필드입니다.
    private final AiClient aiClient;

    // 팀 추천안 생성/목록/상세/승인 상태 변경에 사용하는 Repository 필드입니다.
    private final TeamRecommendationRepository recommendationRepository;

    // 추천안에 포함된 추천 팀원 목록을 조회하는 Repository 필드입니다.
    private final TeamRecommendationMemberRepository recommendationMemberRepository;

    // 추천안의 AI 배정 이유 목록을 조회하는 Repository 필드입니다.
    private final TeamRecommendationReasonRepository recommendationReasonRepository;

    // 추천 수락 시 실제 Team 엔티티를 생성하는 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 추천 수락 시 추천 멤버를 실제 팀원으로 저장하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 추천 상세에서 멤버별 AI 분석 실력 등급을 조회하는 Repository 필드입니다.
    private final UserAnalysisRepository userAnalysisRepository;

    // 팀 승인 시 채팅방을 생성하는 Repository 필드입니다.
    private final ChatRoomRepository chatRoomRepository;

    // 팀 승인 시 기본 채널을 생성하는 Repository 필드입니다.
    private final ChatChannelRepository chatChannelRepository;

    // 해당 학년 미배정 학생을 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 한 팀에 배정할 수 있는 최대 학생 수입니다.
    private static final int MAX_TEAM_MEMBER_COUNT = 5;

    // 재생성 시 이전 추천안과 같은 조합이 반복되지 않도록 다시 섞는 최대 횟수입니다.
    private static final int MAX_REGENERATE_ATTEMPTS = 10;

    // ──────────────────────────────────────────
    // 팀 추천안 생성 (AI 기반)
    // AI 서버에서 팀 매칭 결과를 받아와 해당 학년 학생이 포함된 팀만 추천안으로 저장합니다.
    // ──────────────────────────────────────────
    @Transactional
    public List<TeamRecommendationResponseDto> createRecommendation(TeamRecommendationRequestDto dto) {
        Grade grade = dto.getGrade();

        // 해당 학년 학생 이름 → User 맵 (AI는 이름으로 팀원을 식별하기 때문)
        Set<String> assignedUserIds = teamUserRepository.findAll().stream()
                .map(tu -> tu.getUser().getUserId())
                .collect(Collectors.toSet());

        List<User> gradeStudents = userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, grade)
                .stream()
                .filter(u -> !assignedUserIds.contains(u.getUserId()))
                .toList();

        validateAllStudentsSurveyCompleted(gradeStudents);

        if (gradeStudents.isEmpty()) {
            throw new IllegalStateException("배정할 미배정 학생이 없습니다.");
        }

        // 이름 → User 맵 (중복 이름 있을 경우 grade로 이미 필터된 상태)
        Map<String, User> nameToUser = gradeStudents.stream()
                .collect(Collectors.toMap(User::getName, u -> u, (a, b) -> a));

        // 기존 PENDING 추천안 삭제
        List<TeamRecommendation> existing = recommendationRepository.findByGradeAndStatus(grade, RecommendationStatus.PENDING);
        for (TeamRecommendation rec : existing) {
            recommendationReasonRepository.deleteByRecommendationId(rec.getId());
            recommendationMemberRepository.deleteByRecommendationId(rec.getId());
            recommendationRepository.delete(rec);
        }

        // 백엔드 학생 데이터를 AI 전송용 DTO로 변환
        List<AiStudentPayloadDto> studentPayloads = gradeStudents.stream()
                .map(AiStudentPayloadDto::from)
                .toList();

        // AI 서버 호출: /matching/run 내부에서 분석까지 처리하므로 runMatching만 호출
        AiTeamSummaryResponseDto aiResult;
        try {
            aiResult = aiClient.runMatching(studentPayloads);
        } catch (AiServerException e) {
            log.error("AI 서버 호출 실패. 자체 알고리즘으로 폴백합니다.", e);
            return createRecommendationFallback(grade, gradeStudents);
        }

        // AI 팀 중 해당 학년 학생이 1명 이상 포함된 팀만 추출
        List<AiTeamSummaryResponseDto.TeamDto> targetTeams = aiResult.getTeams().stream()
                .filter(team -> team.getMembers().stream()
                        .anyMatch(m -> nameToUser.containsKey(m.getName())))
                .toList();

        if (targetTeams.isEmpty()) {
            log.warn("AI 결과에서 해당 학년({}) 학생이 포함된 팀이 없습니다. 폴백 실행.", grade);
            return createRecommendationFallback(grade, gradeStudents);
        }

        // AI 팀 결과를 추천안으로 저장
        List<TeamRecommendationResponseDto> result = new ArrayList<>();
        for (AiTeamSummaryResponseDto.TeamDto aiTeam : targetTeams) {

            // 해당 학년 학생만 필터
            List<AiTeamSummaryResponseDto.MemberDto> validMembers = aiTeam.getMembers().stream()
                    .filter(m -> nameToUser.containsKey(m.getName()))
                    .toList();

            if (validMembers.isEmpty()) continue;

            TeamRecommendation recommendation = recommendationRepository.save(
                    TeamRecommendation.builder().grade(grade).build()
            );

            String leaderName = aiTeam.getLeader();
            for (AiTeamSummaryResponseDto.MemberDto m : validMembers) {
                User user = nameToUser.get(m.getName());
                recommendationMemberRepository.save(TeamRecommendationMember.builder()
                        .recommendation(recommendation)
                        .user(user)
                        .studentRole(parseRoleGroup(m.getRoleGroup()))
                        .isRecommendedLeader(m.getName().equals(leaderName))
                        .build());

                // UserAnalysis 저장 (AI skill_level 기반)
                StudentLevel level = parseSkillLevel(m.getSkillLevel());
                userAnalysisRepository.findById(user.getUserId()).ifPresentOrElse(
                        ua -> ua.updateAnalysisResult(m.getSkillLevel(), level),
                        () -> userAnalysisRepository.save(UserAnalysis.builder()
                                .user(user)
                                .analysisResult(m.getSkillLevel())
                                .studentLevel(level)
                                .build())
                );
            }

            // AI가 생성한 팀별 고유 이유 저장
            String description = buildAiDescription(aiTeam);
            recommendationReasonRepository.save(TeamRecommendationReason.builder()
                    .recommendation(recommendation)
                    .title("AI 팀 배정 이유")
                    .description(description)
                    .build());

            result.add(TeamRecommendationResponseDto.from(recommendation));
        }

        return result;
    }

    // AI 역할군 문자열을 StudentRole enum으로 변환하는 기능입니다.
    private StudentRole parseRoleGroup(String roleGroup) {
        if (roleGroup == null) return StudentRole.BACKEND;
        return switch (roleGroup.toLowerCase()) {
            case "frontend" -> StudentRole.FRONTEND;
            case "ai_data" -> StudentRole.AI;
            case "app" -> StudentRole.APP;
            case "game" -> StudentRole.GAME;
            default -> StudentRole.BACKEND;
        };
    }

    // AI skill_level 문자열을 StudentLevel enum으로 변환하는 기능입니다.
    private StudentLevel parseSkillLevel(String skillLevel) {
        if (skillLevel == null) return StudentLevel.MIDDLE;
        return switch (skillLevel) {
            case "상", "높음" -> StudentLevel.UPPER;
            case "하", "낮음" -> StudentLevel.LOWER;
            default -> StudentLevel.MIDDLE;
        };
    }

    // AI 팀 결과로 배정 이유 설명 문자열을 생성하는 기능입니다.
    private String buildAiDescription(AiTeamSummaryResponseDto.TeamDto aiTeam) {
        StringBuilder sb = new StringBuilder();
        if (aiTeam.getMatchingReason() != null && !aiTeam.getMatchingReason().isBlank()) {
            sb.append(aiTeam.getMatchingReason());
        }
        if (aiTeam.getStrengths() != null && !aiTeam.getStrengths().isBlank()) {
            sb.append("\n\n[강점] ").append(aiTeam.getStrengths());
        }
        if (aiTeam.getWeaknesses() != null && !aiTeam.getWeaknesses().isBlank()) {
            sb.append("\n\n[보완점] ").append(aiTeam.getWeaknesses());
        }
        return sb.toString().trim();
    }

    // AI 서버 호출 실패 시 기존 자체 알고리즘으로 팀을 생성하는 폴백 기능입니다.
    private List<TeamRecommendationResponseDto> createRecommendationFallback(Grade grade, List<User> students) {
        List<ScoredStudent> candidates = students.stream()
                .map(u -> new ScoredStudent(u, resolveRole(u), ScoreCalculator.calculate(u)))
                .toList();

        analyzeAndSave(candidates);

        int teamCount = (int) Math.ceil((double) candidates.size() / MAX_TEAM_MEMBER_COUNT);
        List<List<ScoredStudent>> groups = createBalancedGroups(candidates, teamCount, false, Set.of());

        List<TeamRecommendationResponseDto> result = new ArrayList<>();
        for (List<ScoredStudent> group : groups) {
            if (group.isEmpty()) continue;

            TeamRecommendation recommendation = recommendationRepository.save(
                    TeamRecommendation.builder().grade(grade).build()
            );

            ScoredStudent leader = chooseLeader(group);
            for (ScoredStudent s : group) {
                recommendationMemberRepository.save(TeamRecommendationMember.builder()
                        .recommendation(recommendation)
                        .user(s.user())
                        .studentRole(s.role())
                        .isRecommendedLeader(s == leader)
                        .build());
            }

            recommendationReasonRepository.save(TeamRecommendationReason.builder()
                    .recommendation(recommendation)
                    .title("역할 균형 배치")
                    .description(buildReason(group, leader))
                    .build());

            result.add(TeamRecommendationResponseDto.from(recommendation));
        }

        return result;
    }

    // 기존 PENDING 추천안의 팀원 조합을 userId 정렬 문자열로 저장하는 기능입니다.
    private Set<String> collectTeamSignatures(List<TeamRecommendation> recommendations) {
        return recommendations.stream()
                .map(recommendation -> recommendationMemberRepository.findByRecommendationId(recommendation.getId()))
                .filter(members -> !members.isEmpty())
                .map(this::toTeamSignature)
                .collect(Collectors.toSet());
    }

    // 팀 생성 대상 학년의 미배정 학생 전원이 설문을 완료했는지 검증하는 기능입니다.
    private void validateAllStudentsSurveyCompleted(List<User> students) {
        List<User> notCompletedStudents = students.stream()
                .filter(user -> !user.isSurveyCompleted())
                .toList();

        if (!notCompletedStudents.isEmpty()) {
            String names = notCompletedStudents.stream()
                    .map(user -> user.getName() + "(" + user.getUserId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("설문 미완료 학생이 있어 팀을 생성할 수 없습니다: " + names);
        }
    }

    // 스코어 기준으로 상/중/하 등급을 분류하고 UserAnalysis에 저장하는 기능입니다.
    private void analyzeAndSave(List<ScoredStudent> candidates) {
        List<ScoredStudent> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(ScoredStudent::score).reversed()
                        .thenComparing(s -> s.user().getUserId()))
                .toList();

        int upperCount = (int) Math.ceil(sorted.size() * 0.2);
        int lowerStart = Math.max(sorted.size() - upperCount, upperCount);

        for (int i = 0; i < sorted.size(); i++) {
            StudentLevel level;
            if (i < upperCount) level = StudentLevel.UPPER;
            else if (i >= lowerStart) level = StudentLevel.LOWER;
            else level = StudentLevel.MIDDLE;

            ScoredStudent s = sorted.get(i);
            String analysis = "%s 역할 희망, 스킬 %d개, 경험 %d개 기준 %s 등급으로 분석되었습니다."
                    .formatted(s.role().name(), safeSize(s.user().getSkill()), safeSize(s.user().getExperience()), toKorean(level));

            userAnalysisRepository.findById(s.user().getUserId())
                    .ifPresentOrElse(
                            ua -> ua.updateAnalysisResult(analysis, level),
                            () -> userAnalysisRepository.saveAndFlush(UserAnalysis.builder()
                                    .user(s.user())
                                    .analysisResult(analysis)
                                    .studentLevel(level)
                                    .build())
                    );
        }
    }

    // 최초 생성은 안정적으로, 재생성은 균형을 유지하면서 이전과 다른 조합이 나오도록 배분하는 기능입니다.
    private List<List<ScoredStudent>> createBalancedGroups(
            List<ScoredStudent> candidates,
            int teamCount,
            boolean regenerate,
            Set<String> previousTeamSignatures
    ) {
        List<List<ScoredStudent>> groups = List.of();
        int attempts = regenerate ? MAX_REGENERATE_ATTEMPTS : 1;

        for (int attempt = 0; attempt < attempts; attempt++) {
            Random random = regenerate ? new Random(System.nanoTime() + attempt) : null;
            groups = distributeByRole(candidates, teamCount, random);
            if (!regenerate || !hasSameTeamCombination(groups, previousTeamSignatures)) {
                return groups;
            }
        }

        return groups;
    }

    // 역할별 라운드로빈으로 팀 수만큼 그룹에 배분하는 기능입니다.
    private List<List<ScoredStudent>> distributeByRole(List<ScoredStudent> candidates, int teamCount, Random random) {
        List<List<ScoredStudent>> groups = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) groups.add(new ArrayList<>());

        Map<StudentRole, Queue<ScoredStudent>> roleQueues = new EnumMap<>(StudentRole.class);
        candidates.stream()
                .collect(Collectors.groupingBy(ScoredStudent::role, () -> new EnumMap<>(StudentRole.class), Collectors.toList()))
                .forEach((role, list) -> roleQueues.put(role, new ArrayDeque<>(sortAndMaybeShuffleSimilarScores(list, random))));

        List<StudentRole> roleOrder = List.of(
                StudentRole.BACKEND, StudentRole.FRONTEND, StudentRole.AI, StudentRole.APP, StudentRole.DESIGN
        );

        int idx = 0;
        for (StudentRole role : roleOrder) {
            Queue<ScoredStudent> queue = roleQueues.getOrDefault(role, new ArrayDeque<>());
            while (!queue.isEmpty()) {
                groups.get(idx % teamCount).add(queue.poll());
                idx++;
            }
        }
        return groups;
    }

    // 같은 역할 안에서는 점수 순서를 유지하되 재생성 시 비슷한 점수대끼리만 섞는 기능입니다.
    private List<ScoredStudent> sortAndMaybeShuffleSimilarScores(List<ScoredStudent> students, Random random) {
        List<ScoredStudent> sorted = students.stream()
                .sorted(Comparator.comparingDouble(ScoredStudent::score).reversed()
                        .thenComparing(s -> s.user().getUserId()))
                .toList();

        if (random == null) {
            return sorted;
        }

        TreeMap<Integer, List<ScoredStudent>> scoreBuckets = sorted.stream()
                .collect(Collectors.groupingBy(
                        student -> scoreBucket(student.score()),
                        TreeMap::new,
                        Collectors.toCollection(ArrayList::new)
                ));

        List<ScoredStudent> shuffled = new ArrayList<>();
        scoreBuckets.descendingMap().values().forEach(bucket -> {
            Collections.shuffle(bucket, random);
            shuffled.addAll(bucket);
        });
        return shuffled;
    }

    // 점수 차이가 크지 않은 학생끼리만 섞기 위해 5점 단위 점수 구간을 계산하는 기능입니다.
    private int scoreBucket(double score) {
        return (int) Math.floor(score / 5.0);
    }

    // 새로 만든 팀 중 기존 추천안과 완전히 같은 팀원 조합이 있는지 확인하는 기능입니다.
    private boolean hasSameTeamCombination(List<List<ScoredStudent>> groups, Set<String> previousTeamSignatures) {
        if (previousTeamSignatures.isEmpty()) {
            return false;
        }

        return groups.stream()
                .map(this::toScoredTeamSignature)
                .anyMatch(previousTeamSignatures::contains);
    }

    // 추천 멤버 목록을 정렬된 userId 조합 문자열로 변환하는 기능입니다.
    private String toTeamSignature(List<TeamRecommendationMember> members) {
        return members.stream()
                .map(member -> member.getUser().getUserId())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    // 점수 계산된 팀원 목록을 정렬된 userId 조합 문자열로 변환하는 기능입니다.
    private String toScoredTeamSignature(List<ScoredStudent> members) {
        return members.stream()
                .map(member -> member.user().getUserId())
                .sorted()
                .collect(Collectors.joining("|"));
    }

    // 팀장 희망자 중 점수 최고, 없으면 전체 중 점수 최고를 팀장으로 선정하는 기능입니다.
    private ScoredStudent chooseLeader(List<ScoredStudent> group) {
        return group.stream()
                .filter(s -> s.user().isWantsLeader())
                .max(Comparator.comparingDouble(ScoredStudent::score))
                .orElseGet(() -> group.stream()
                        .max(Comparator.comparingDouble(ScoredStudent::score))
                        .orElseThrow());
    }

    // 팀 구성 특성을 반영한 배정 이유 문자열을 생성하는 기능입니다.
    private String buildReason(List<ScoredStudent> group, ScoredStudent leader) {
        // 역할 분포
        String roles = group.stream()
                .map(s -> toKoreanRole(s.role()))
                .distinct()
                .collect(Collectors.joining(", "));

        // 팀 내 대표 기술 스택 최대 3개
        String topSkills = group.stream()
                .flatMap(s -> safeList(s.user().getSkill()).stream())
                .distinct()
                .limit(3)
                .collect(Collectors.joining(", "));

        // 팀장 희망 여부
        String leaderNote = leader.user().isWantsLeader()
                ? leader.user().getName() + "(팀장 희망)"
                : leader.user().getName() + "(점수 최고)";

        return roles + " 역할로 구성되었으며, " +
                (topSkills.isEmpty() ? "" : topSkills + " 기술을 보유한 팀입니다. ") +
                leaderNote + "을(를) 팀장으로 추천합니다.";
    }

    // StudentRole enum을 한국어로 변환하는 기능입니다.
    private String toKoreanRole(StudentRole role) {
        return switch (role) {
            case BACKEND -> "백엔드";
            case FRONTEND -> "프론트엔드";
            case AI -> "AI";
            case APP -> "앱";
            case DESIGN -> "디자인";
            case DEVOPS -> "DevOps";
            case GAME -> "게임개발";
            case FULLSTACK -> "풀스택";
            case SECURITY -> "보안";
        };
    }

    private List<String> safeList(List<String> list) {
        return list == null ? List.of() : list;
    }

    // User의 희망 역할을 반환하며 미설정 시 BACKEND로 기본 처리하는 기능입니다.
    private StudentRole resolveRole(User user) {
        return user.getStudentRole() != null ? user.getStudentRole() : StudentRole.BACKEND;
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private String toKorean(StudentLevel level) {
        return switch (level) {
            case UPPER -> "상";
            case MIDDLE -> "중";
            case LOWER -> "하";
        };
    }

    private record ScoredStudent(User user, StudentRole role, double score) {}

    // ──────────────────────────────────────────
    // 추천 목록 조회
    // ──────────────────────────────────────────
    public List<TeamRecommendationResponseDto> getRecommendationList() {
        return recommendationRepository.findAll().stream()
                .map(TeamRecommendationResponseDto::from)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // 추천 상세 조회 (멤버 + 배정 이유 포함)
    // ──────────────────────────────────────────
    public TeamRecommendationDetailResponseDto getRecommendationDetail(Long recommendationId) {
        TeamRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("추천안을 찾을 수 없습니다."));

        List<TeamRecommendationMember> members =
                recommendationMemberRepository.findByRecommendationId(recommendationId);

        List<TeamRecommendationReason> reasons =
                recommendationReasonRepository.findByRecommendationId(recommendationId);

        // 멤버별 studentLevel 맵 생성 (userId → studentLevel)
        Map<String, StudentLevel> levelMap = members.stream()
                .collect(Collectors.toMap(
                        m -> m.getUser().getUserId(),
                        m -> userAnalysisRepository.findById(m.getUser().getUserId())
                                .map(UserAnalysis::getStudentLevel)
                                .orElse(null)
                ));

        return TeamRecommendationDetailResponseDto.from(recommendation, members, reasons, levelMap);
    }

    // ──────────────────────────────────────────
    // 추천 수락 → 실제 팀 생성
    // ──────────────────────────────────────────
    @Transactional
    public void acceptRecommendation(Long recommendationId) {
        TeamRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("추천안을 찾을 수 없습니다."));

        validatePendingRecommendation(recommendation);

        // 추천 멤버를 먼저 조회하고 설문 완료 여부와 최대 인원 수를 검증한 뒤 실제 팀을 생성합니다.
        List<TeamRecommendationMember> recommendedMembers =
                recommendationMemberRepository.findByRecommendationId(recommendationId);
        validateRecommendedMembers(recommendedMembers);

        // 해당 학년에서 몇 번째 팀인지 계산해서 팀 이름 자동 생성 (1팀, 2팀...)
        long teamCount = teamRepository.countByGrade(recommendation.getGrade());
        String teamName = (teamCount + 1) + "팀";

        // 팀 생성
        Team team = Team.builder()
                .teamName(teamName)
                .grade(recommendation.getGrade())
                .status(TeamStatus.APPROVED)
                .build();
        teamRepository.save(team);

        // 추천 멤버들을 실제 팀원으로 등록
        for (TeamRecommendationMember recommendedMember : recommendedMembers) {
            TeamUser teamUser = TeamUser.builder()
                    .team(team)
                    .user(recommendedMember.getUser())
                    .studentRole(recommendedMember.getStudentRole())
                    .leaderRole(recommendedMember.isRecommendedLeader() ? LeaderRole.LEADER : LeaderRole.MEMBER)
                    .build();
            teamUserRepository.save(teamUser);
        }

        // TODO: 프론트 채팅 개발 완료 후 주석 해제
//        User channelCreator = recommendedMembers.stream()
//                .filter(m -> m.isRecommendedLeader())
//                .findFirst()
//                .map(TeamRecommendationMember::getUser)
//                .orElse(recommendedMembers.get(0).getUser());
//
//        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
//                .team(team)
//                .build());
//        chatChannelRepository.save(ChatChannel.builder()
//                .chatRoom(chatRoom)
//                .channelName("공통")
//                .createdBy(channelCreator)
//                .build());

        // 추천안 상태 수락으로 변경 (더티 체킹)
        recommendation.accept();
    }

    // 승인 대기 상태인 추천안만 실제 팀으로 전환할 수 있게 검증하는 기능입니다.
    private void validatePendingRecommendation(TeamRecommendation recommendation) {
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalArgumentException("이미 승인된 추천안입니다.");
        }
    }

    // 추천 멤버 수와 설문 완료 여부를 검증해 잘못된 팀 생성을 막는 기능입니다.
    private void validateRecommendedMembers(List<TeamRecommendationMember> recommendedMembers) {
        if (recommendedMembers.isEmpty()) {
            throw new IllegalArgumentException("추천안에 팀원이 없습니다.");
        }

        if (recommendedMembers.size() > MAX_TEAM_MEMBER_COUNT) {
            throw new IllegalArgumentException("팀 인원은 최대 5명까지 가능합니다.");
        }

        List<User> notCompletedStudents = recommendedMembers.stream()
                .map(TeamRecommendationMember::getUser)
                .filter(user -> !user.isSurveyCompleted())
                .toList();
        if (!notCompletedStudents.isEmpty()) {
            String names = notCompletedStudents.stream()
                    .map(user -> user.getName() + "(" + user.getUserId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("설문 미완료 학생이 있어 팀을 생성할 수 없습니다: " + names);
        }
    }

    // ──────────────────────────────────────────
    // 학년별 추천 상세 목록 조회 (프론트 팀 검토 화면용)
    // ──────────────────────────────────────────
    public List<TeamRecommendationDetailResponseDto> getRecommendationsByGrade(Grade grade) {
        return recommendationRepository.findByGrade(grade).stream()
                .map(rec -> {
                    List<TeamRecommendationMember> members =
                            recommendationMemberRepository.findByRecommendationId(rec.getId());
                    List<TeamRecommendationReason> reasons =
                            recommendationReasonRepository.findByRecommendationId(rec.getId());
                    Map<String, StudentLevel> levelMap = members.stream()
                            .collect(Collectors.toMap(
                                    m -> m.getUser().getUserId(),
                                    m -> userAnalysisRepository.findById(m.getUser().getUserId())
                                            .map(UserAnalysis::getStudentLevel)
                                            .orElse(null)
                            ));
                    return TeamRecommendationDetailResponseDto.from(rec, members, reasons, levelMap);
                })
                .toList();
    }

    // ──────────────────────────────────────────
    // 추천안 두 학생 교환 (swap)
    // ──────────────────────────────────────────
    @Transactional
    public void swapMembers(SwapRecommendationMembersRequestDto dto) {
        TeamRecommendationMember from = recommendationMemberRepository
                .findByRecommendationIdAndUserUserId(dto.getFromRecommendationId(), dto.getFromUserId())
                .orElseThrow(() -> new IllegalArgumentException("교환할 학생 A를 찾을 수 없습니다."));

        TeamRecommendationMember to = recommendationMemberRepository
                .findByRecommendationIdAndUserUserId(dto.getToRecommendationId(), dto.getToUserId())
                .orElseThrow(() -> new IllegalArgumentException("교환할 학생 B를 찾을 수 없습니다."));

        TeamRecommendation fromRec = from.getRecommendation();
        TeamRecommendation toRec = to.getRecommendation();

        recommendationMemberRepository.delete(from);
        recommendationMemberRepository.delete(to);
        recommendationMemberRepository.flush();

        recommendationMemberRepository.save(TeamRecommendationMember.builder()
                .recommendation(toRec)
                .user(from.getUser())
                .studentRole(from.getStudentRole())
                .isRecommendedLeader(false)
                .build());

        recommendationMemberRepository.save(TeamRecommendationMember.builder()
                .recommendation(fromRec)
                .user(to.getUser())
                .studentRole(to.getStudentRole())
                .isRecommendedLeader(false)
                .build());
    }

    // ──────────────────────────────────────────
    // 학년 전체 추천안 일괄 수락 → 팀 생성
    // ──────────────────────────────────────────
    @Transactional
    public void acceptAllByGrade(Grade grade) {
        List<TeamRecommendation> pending = recommendationRepository.findByGrade(grade).stream()
                .filter(r -> r.getStatus() == RecommendationStatus.PENDING)
                .toList();

        if (pending.isEmpty()) {
            throw new IllegalStateException("수락할 PENDING 상태의 추천안이 없습니다.");
        }

        for (TeamRecommendation rec : pending) {
            acceptRecommendation(rec.getId());
        }
    }

}
