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
import com.capteam.gaobackend.exception.MatchingJobCancelledException;
import com.capteam.gaobackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
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

    private final AdminTeamRecommendationPersistenceService recommendationPersistenceService;
    private final AdminTeamMatchingPreparationService matchingPreparationService;
    private final TeamAssignmentNoticeService teamAssignmentNoticeService;

    private static final int MAX_TEAM_MEMBER_COUNT = 5;

    // ──────────────────────────────────────────
    // 팀 추천안 생성 (AI 기반)
    // AI 서버에서 팀 매칭 결과를 받아와 해당 학년 학생이 포함된 팀만 추천안으로 저장합니다.
    // ──────────────────────────────────────────
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TeamRecommendationResponseDto> createRecommendation(TeamRecommendationRequestDto dto) {
        return createRecommendation(dto.getGrade(), normalizePrompt(dto.getRegenerationPrompt()), null, () -> true);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TeamRecommendationResponseDto> createRecommendation(
            Grade grade,
            String jobId,
            BooleanSupplier beginCompletion
    ) {
        return createRecommendation(grade, null, jobId, beginCompletion);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<TeamRecommendationResponseDto> createRecommendation(
            Grade grade,
            String regenerationPrompt,
            String jobId,
            BooleanSupplier beginCompletion
    ) {

        // 학생 조회 트랜잭션은 준비 단계에서 종료해 긴 AI 호출 동안 DB 연결을 점유하지 않습니다.
        AdminTeamMatchingPreparationService.PreparedMatching prepared = matchingPreparationService.prepare(grade);
        Map<String, String> nameToUserId = prepared.nameToUserId();
        List<AiStudentPayloadDto> studentPayloads = prepared.studentPayloads();

        // AI 서버 호출: /matching/run 내부에서 분석까지 처리하므로 runMatching만 호출
        AiTeamSummaryResponseDto aiResult;
        try {
            aiResult = jobId == null
                    ? aiClient.runMatchingWithPrompt(studentPayloads, regenerationPrompt)
                    : aiClient.runMatching(studentPayloads, jobId, regenerationPrompt);
        } catch (AiServerException e) {
            log.error("AI 서버 호출 실패.", e);
            throw new IllegalStateException("AI 서버 호출에 실패했습니다. AI 서버 상태를 확인해주세요.", e);
        }

        // AI 팀 중 해당 학년 학생이 1명 이상 포함된 팀만 추출
        List<AiTeamSummaryResponseDto.TeamDto> targetTeams = aiResult.getTeams().stream()
                .filter(team -> team.getMembers().stream()
                        .anyMatch(m -> nameToUserId.containsKey(m.getName())))
                .toList();

        if (targetTeams.isEmpty()) {
            log.warn("AI 결과에서 해당 학년({}) 학생 이름이 매칭되지 않았습니다. AI 반환 이름: {}, 백엔드 이름: {}",
                    grade,
                    aiResult.getTeams().stream().flatMap(t -> t.getMembers().stream()).map(AiTeamSummaryResponseDto.MemberDto::getName).toList(),
                    nameToUserId.keySet());
            throw new IllegalStateException("AI 매칭 결과와 백엔드 학생 이름이 일치하지 않습니다. AI 서버 로그를 확인해주세요.");
        }

        if (!beginCompletion.getAsBoolean()) {
            throw new MatchingJobCancelledException(jobId);
        }
        // 저장 단계만 별도 트랜잭션으로 실행해 전체 추천안 교체를 원자적으로 처리합니다.
        return recommendationPersistenceService.replacePendingRecommendations(grade, nameToUserId, targetTeams);
    }

    private String normalizePrompt(String regenerationPrompt) {
        if (regenerationPrompt == null || regenerationPrompt.isBlank()) {
            return null;
        }
        return regenerationPrompt.trim();
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
        Team team = acceptRecommendationAndCreateTeam(recommendationId);

        // 개별 승인 방식에서도 해당 학년의 마지막 추천안을 승인한 시점에 최종 결과 공지를 생성합니다.
        if (recommendationRepository
                .findByGradeAndStatus(team.getGrade(), RecommendationStatus.PENDING)
                .isEmpty()) {
            teamAssignmentNoticeService.createNotice(team.getGrade());
        }
    }

    private Team acceptRecommendationAndCreateTeam(Long recommendationId) {
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
                .strengths(recommendation.getStrengths())
                .weaknesses(recommendation.getWeaknesses())
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
        return team;
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

        List<User> alreadyAssignedStudents = recommendedMembers.stream()
                .map(TeamRecommendationMember::getUser)
                .filter(user -> teamUserRepository.existsByUserUserId(user.getUserId()))
                .toList();
        if (!alreadyAssignedStudents.isEmpty()) {
            String names = alreadyAssignedStudents.stream()
                    .map(user -> user.getName() + "(" + user.getUserId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("이미 팀에 배정된 학생이 있어 팀을 생성할 수 없습니다: " + names);
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
            acceptRecommendationAndCreateTeam(rec.getId());
        }

        teamAssignmentNoticeService.createNotice(grade);
    }

}
