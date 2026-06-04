package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.team.SwapRecommendationMembersRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.*;
import com.capteam.gaobackend.repository.*;
import com.capteam.gaobackend.util.ScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamRecommendationService {

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

    private static final int TEAM_SIZE = 5;

    // ──────────────────────────────────────────
    // 팀 추천안 생성
    // 해당 학년 미배정 학생을 역할별 라운드로빈으로 팀에 배분하고 추천안으로 저장합니다.
    // ──────────────────────────────────────────
    @Transactional
    public List<TeamRecommendationResponseDto> createRecommendation(TeamRecommendationRequestDto dto) {
        Grade grade = dto.getGrade();

        // 재생성 시 해당 학년의 기존 PENDING 추천안 전부 삭제
        List<TeamRecommendation> existing = recommendationRepository.findByGradeAndStatus(grade, RecommendationStatus.PENDING);
        for (TeamRecommendation rec : existing) {
            recommendationReasonRepository.deleteByRecommendationId(rec.getId());
            recommendationMemberRepository.deleteByRecommendationId(rec.getId());
            recommendationRepository.delete(rec);
        }

        // 이미 팀에 배정된 학생 userId 목록
        Set<String> assignedUserIds = teamUserRepository.findAll().stream()
                .map(tu -> tu.getUser().getUserId())
                .collect(Collectors.toSet());

        // 해당 학년 미배정 학생만 추출 후 스코어 계산
        List<ScoredStudent> candidates = userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, grade)
                .stream()
                .filter(u -> !assignedUserIds.contains(u.getUserId()))
                .sorted(Comparator.comparing(User::getUserId))
                .map(u -> new ScoredStudent(u, resolveRole(u), ScoreCalculator.calculate(u)))
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalStateException("배정할 미배정 학생이 없습니다.");
        }

        // 실력 등급 분류 후 UserAnalysis 저장
        analyzeAndSave(candidates);

        // 팀 수 계산 후 역할별 라운드로빈 배분
        int teamCount = (int) Math.ceil((double) candidates.size() / TEAM_SIZE);
        List<List<ScoredStudent>> groups = distributeByRole(candidates, teamCount);

        // 각 그룹을 추천안으로 저장
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

            userAnalysisRepository.findByUserUserId(s.user().getUserId())
                    .ifPresentOrElse(
                            ua -> ua.updateAnalysisResult(analysis, level),
                            () -> userAnalysisRepository.save(UserAnalysis.builder()
                                    .user(s.user())
                                    .analysisResult(analysis)
                                    .studentLevel(level)
                                    .build())
                    );
        }
    }

    // 역할별 라운드로빈으로 팀 수만큼 그룹에 배분하는 기능입니다.
    private List<List<ScoredStudent>> distributeByRole(List<ScoredStudent> candidates, int teamCount) {
        List<List<ScoredStudent>> groups = new ArrayList<>();
        for (int i = 0; i < teamCount; i++) groups.add(new ArrayList<>());

        Map<StudentRole, Queue<ScoredStudent>> roleQueues = new EnumMap<>(StudentRole.class);
        candidates.stream()
                .collect(Collectors.groupingBy(ScoredStudent::role, () -> new EnumMap<>(StudentRole.class), Collectors.toList()))
                .forEach((role, list) -> roleQueues.put(role,
                        new ArrayDeque<>(list.stream()
                                .sorted(Comparator.comparingDouble(ScoredStudent::score).reversed()
                                        .thenComparing(s -> s.user().getUserId()))
                                .toList())));

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

    // 팀장 희망자 중 점수 최고, 없으면 전체 중 점수 최고를 팀장으로 선정하는 기능입니다.
    private ScoredStudent chooseLeader(List<ScoredStudent> group) {
        return group.stream()
                .filter(s -> s.user().isWantsLeader())
                .max(Comparator.comparingDouble(ScoredStudent::score))
                .orElseGet(() -> group.stream()
                        .max(Comparator.comparingDouble(ScoredStudent::score))
                        .orElseThrow());
    }

    // 팀 배정 이유 문자열을 생성하는 기능입니다.
    private String buildReason(List<ScoredStudent> group, ScoredStudent leader) {
        String roles = group.stream()
                .map(s -> s.role().name())
                .distinct()
                .collect(Collectors.joining(", "));
        return roles + " 역할을 균형 있게 배치하고, " + leader.user().getName() + "을(를) 팀장으로 추천합니다.";
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
                        m -> userAnalysisRepository.findByUserUserId(m.getUser().getUserId())
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
        List<TeamRecommendationMember> recommendedMembers =
                recommendationMemberRepository.findByRecommendationId(recommendationId);

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
                                    m -> userAnalysisRepository.findByUserUserId(m.getUser().getUserId())
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
