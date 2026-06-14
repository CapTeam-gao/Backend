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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    private static final int MAX_TEAM_MEMBER_COUNT = 5;

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
            log.error("AI 서버 호출 실패.", e);
            throw new IllegalStateException("AI 서버 호출에 실패했습니다. AI 서버 상태를 확인해주세요.", e);
        }

        // AI 팀 중 해당 학년 학생이 1명 이상 포함된 팀만 추출
        List<AiTeamSummaryResponseDto.TeamDto> targetTeams = aiResult.getTeams().stream()
                .filter(team -> team.getMembers().stream()
                        .anyMatch(m -> nameToUser.containsKey(m.getName())))
                .toList();

        if (targetTeams.isEmpty()) {
            log.warn("AI 결과에서 해당 학년({}) 학생 이름이 매칭되지 않았습니다. AI 반환 이름: {}, 백엔드 이름: {}",
                    grade,
                    aiResult.getTeams().stream().flatMap(t -> t.getMembers().stream()).map(AiTeamSummaryResponseDto.MemberDto::getName).toList(),
                    nameToUser.keySet());
            throw new IllegalStateException("AI 매칭 결과와 백엔드 학생 이름이 일치하지 않습니다. AI 서버 로그를 확인해주세요.");
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
                        .studentRole(parseRoleGroup(m.getRoleGroup(), m.getRole()))
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
                    .title("팀 배정 이유")
                    .description(description)
                    .build());

            result.add(TeamRecommendationResponseDto.from(recommendation));
        }

        return result;
    }

    // AI 역할군 문자열을 StudentRole enum으로 변환하는 기능입니다.
    private StudentRole parseRoleGroup(String roleGroup, String role) {
        String normalizedRoleGroup = roleGroup == null ? "" : roleGroup.toLowerCase(Locale.ROOT);
        StudentRole parsed = switch (normalizedRoleGroup) {
            case "frontend" -> StudentRole.FRONTEND;
            case "ai_data" -> StudentRole.AI;
            case "app" -> StudentRole.APP;
            case "game" -> StudentRole.GAME;
            case "backend" -> StudentRole.BACKEND;
            default -> null;
        };
        if (parsed != null) {
            return parsed;
        }

        String normalizedRole = role == null ? "" : role.toLowerCase(Locale.ROOT);
        if (containsAny(normalizedRole, "frontend", "front", "프론트", "react", "vue")) {
            return StudentRole.FRONTEND;
        }
        if (containsAny(normalizedRole, "ai", "데이터", "머신러닝", "ml", "pytorch", "tensorflow", "langchain")) {
            return StudentRole.AI;
        }
        if (containsAny(normalizedRole, "app", "android", "ios", "flutter", "모바일", "앱")) {
            return StudentRole.APP;
        }
        if (containsAny(normalizedRole, "design", "figma", "ui/ux", "uiux", "디자인")) {
            return StudentRole.DESIGN;
        }
        return StudentRole.BACKEND;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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
        if (aiTeam.getMatchingReason() == null || aiTeam.getMatchingReason().isBlank()) {
            return "";
        }

        return aiTeam.getMatchingReason()
                .split("\\s*\\[(강점|보완점|약점|리스크)]", 2)[0]
                .trim();
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

        createTeamChatRoomWithDefaultChannel(team, recommendedMembers);

        // 추천안 상태 수락으로 변경 (더티 체킹)
        recommendation.accept();
    }

    // 팀 생성 완료 후 팀 채팅방과 기본 공통 채널을 생성하는 기능입니다.
    private void createTeamChatRoomWithDefaultChannel(Team team, List<TeamRecommendationMember> recommendedMembers) {
        if (chatRoomRepository.findByTeamId(team.getId()).isPresent()) {
            return;
        }

        User channelCreator = recommendedMembers.stream()
                .filter(TeamRecommendationMember::isRecommendedLeader)
                .findFirst()
                .map(TeamRecommendationMember::getUser)
                .orElseGet(() -> recommendedMembers.get(0).getUser());

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.builder()
                .team(team)
                .build());
        chatChannelRepository.save(ChatChannel.builder()
                .chatRoom(chatRoom)
                .channelName("공통")
                .createdBy(channelCreator)
                .build());
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
