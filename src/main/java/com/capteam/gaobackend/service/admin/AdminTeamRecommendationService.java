package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminTeamRecommendationService {

    private final TeamRecommendationRepository recommendationRepository;
    private final TeamRecommendationMemberRepository recommendationMemberRepository;
    private final TeamRecommendationReasonRepository recommendationReasonRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    // ──────────────────────────────────────────
    // AI 팀 추천 요청
    // 실제 AI 연동은 나중에 AiClient에서 처리, 지금은 추천안 생성만
    // ──────────────────────────────────────────
    @Transactional
    public TeamRecommendationResponseDto createRecommendation(TeamRecommendationRequestDto dto) {
        TeamRecommendation recommendation = TeamRecommendation.builder()
                .grade(dto.getGrade())
                .build();

        recommendationRepository.save(recommendation);

        // TODO: AI 연동 후 멤버 배정 및 이유 저장 로직 추가
        return TeamRecommendationResponseDto.from(recommendation);
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

        return TeamRecommendationDetailResponseDto.from(recommendation, members, reasons);
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
            TeamMember teamMember = TeamMember.builder()
                    .team(team)
                    .user(recommendedMember.getUser())
                    .studentRole(recommendedMember.getStudentRole())
                    .leaderRole(LeaderRole.MEMBER) // 기본값 MEMBER, 나중에 팀장 지정
                    .build();
            teamMemberRepository.save(teamMember);
        }

        // 추천안 상태 수락으로 변경 (더티 체킹)
        recommendation.accept();
    }

    // ──────────────────────────────────────────
    // 추천 거절
    // ──────────────────────────────────────────
    @Transactional
    public void rejectRecommendation(Long recommendationId) {
        TeamRecommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("추천안을 찾을 수 없습니다."));

        // 추천안 상태 거절로 변경 (더티 체킹)
        recommendation.reject();
    }
}
