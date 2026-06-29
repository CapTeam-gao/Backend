package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.entity.TeamRecommendationMember;
import com.capteam.gaobackend.entity.TeamRecommendationReason;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.RecommendationStatus;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamRecommendationMemberRepository;
import com.capteam.gaobackend.repository.TeamRecommendationReasonRepository;
import com.capteam.gaobackend.repository.TeamRecommendationRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTeamRecommendationPersistenceService {

    private final TeamRecommendationRepository recommendationRepository;
    private final TeamRecommendationMemberRepository recommendationMemberRepository;
    private final TeamRecommendationReasonRepository recommendationReasonRepository;
    private final UserRepository userRepository;

    // 취소 검증을 통과한 AI 결과만 하나의 트랜잭션에서 기존 추천안과 교체합니다.
    @Transactional
    public List<TeamRecommendationResponseDto> replacePendingRecommendations(
            Grade grade,
            Map<String, String> nameToUserId,
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams
    ) {
        Map<String, User> usersById = userRepository.findAllById(nameToUserId.values()).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
        List<TeamRecommendation> existing = recommendationRepository
                .findByGradeAndStatus(grade, RecommendationStatus.PENDING);
        for (TeamRecommendation recommendation : existing) {
            recommendationReasonRepository.deleteByRecommendationId(recommendation.getId());
            recommendationMemberRepository.deleteByRecommendationId(recommendation.getId());
            recommendationRepository.delete(recommendation);
        }

        List<TeamRecommendationResponseDto> result = new ArrayList<>();
        for (AiTeamSummaryResponseDto.TeamDto aiTeam : targetTeams) {
            List<AiTeamSummaryResponseDto.MemberDto> validMembers = aiTeam.getMembers().stream()
                    .filter(member -> AiTeamMemberUserResolver.resolveUserId(member, nameToUserId) != null)
                    .toList();

            if (validMembers.isEmpty()) {
                continue;
            }

            TeamRecommendation recommendation = recommendationRepository.save(
                    TeamRecommendation.builder()
                            .grade(grade)
                            .strengths(aiTeam.getStrengths())
                            .weaknesses(aiTeam.getWeaknesses())
                            .build()
            );

            String leaderName = aiTeam.getLeader();
            for (AiTeamSummaryResponseDto.MemberDto member : validMembers) {
                String userId = AiTeamMemberUserResolver.resolveUserId(member, nameToUserId);
                User user = usersById.get(userId);
                if (user == null) {
                    throw new IllegalStateException("추천 대상 학생을 찾을 수 없습니다: " + userId);
                }
                recommendationMemberRepository.save(TeamRecommendationMember.builder()
                        .recommendation(recommendation)
                        .user(user)
                        .studentRole(parseRoleGroup(member.getRoleGroup(), member.getRole()))
                        .isRecommendedLeader(AiTeamMemberUserResolver.isSameStudent(member, leaderName, nameToUserId))
                        .build());
            }

            saveRecommendationReasons(recommendation, aiTeam);

            result.add(TeamRecommendationResponseDto.from(recommendation));
        }
        return result;
    }

    private void saveRecommendationReasons(
            TeamRecommendation recommendation,
            AiTeamSummaryResponseDto.TeamDto aiTeam
    ) {
        boolean savedAnyReason = false;
        List<AiTeamSummaryResponseDto.ReasonCardDto> reasonCards = aiTeam.getReasonCards();
        if (reasonCards != null && !reasonCards.isEmpty()) {
            for (AiTeamSummaryResponseDto.ReasonCardDto reasonCard : reasonCards) {
                String title = reasonCard.getTitle() == null ? "" : reasonCard.getTitle().trim();
                String description = cleanAiDescription(reasonCard.getDescription());
                if (title.isBlank() || description.isBlank()) {
                    continue;
                }

                recommendationReasonRepository.save(TeamRecommendationReason.builder()
                        .recommendation(recommendation)
                        .title(title)
                        .description(description)
                        .build());
                savedAnyReason = true;
            }
        }

        if (!savedAnyReason) {
            recommendationReasonRepository.save(TeamRecommendationReason.builder()
                    .recommendation(recommendation)
                    .title("팀 배정 이유")
                    .description(buildAiDescription(aiTeam))
                    .build());
        }
    }

    private StudentRole parseRoleGroup(String roleGroup, String role) {
        String normalizedRoleGroup = normalizeRoleText(roleGroup);
        StudentRole specializedRoleGroup = parseSpecializedRole(normalizedRoleGroup);
        if (specializedRoleGroup != null) {
            return specializedRoleGroup;
        }

        String normalizedRole = normalizeRoleText(role);
        StudentRole specializedRole = parseSpecializedRole(normalizedRole);
        if (specializedRole != null) {
            return specializedRole;
        }

        StudentRole parsed = switch (normalizedRoleGroup) {
            case "frontend", "front" -> StudentRole.FRONTEND;
            case "ai_data", "ai", "data" -> StudentRole.AI;
            case "app" -> StudentRole.APP;
            case "game" -> StudentRole.GAME;
            case "backend" -> StudentRole.BACKEND;
            default -> null;
        };
        if (parsed != null) {
            return parsed;
        }

        if (containsAny(normalizedRole, "game", "게임")) {
            return StudentRole.GAME;
        }
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

    private StudentRole parseSpecializedRole(String normalizedRole) {
        if (containsAny(normalizedRole, "fullstack", "full_stack", "풀스택")) {
            return StudentRole.FULLSTACK;
        }
        if (containsAny(normalizedRole, "devops", "dev_ops", "인프라")) {
            return StudentRole.DEVOPS;
        }
        if (containsAny(normalizedRole, "security", "보안", "시큐리티")) {
            return StudentRole.SECURITY;
        }

        return null;
    }

    private String normalizeRoleText(String role) {
        if (role == null) {
            return "";
        }

        return role.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private StudentLevel parseSkillLevel(String skillLevel) {
        if (skillLevel == null) {
            return StudentLevel.MIDDLE;
        }
        return switch (skillLevel.trim().toUpperCase()) {
            case "상", "높음" -> StudentLevel.UPPER;
            // 팀 추천 결과의 skill_level도 학생 분석과 같은 중상/중하 enum으로 정규화합니다.
            case "UPPER_MIDDLE", "UPPER-MIDDLE", "HIGH_MIDDLE", "HIGH-MIDDLE", "중상" -> StudentLevel.UPPER_MIDDLE;
            case "LOWER_MIDDLE", "LOWER-MIDDLE", "LOW_MIDDLE", "LOW-MIDDLE", "중하" -> StudentLevel.LOWER_MIDDLE;
            case "하", "낮음" -> StudentLevel.LOWER;
            default -> StudentLevel.MIDDLE;
        };
    }

    private String buildAiDescription(AiTeamSummaryResponseDto.TeamDto aiTeam) {
        return cleanAiDescription(aiTeam.getMatchingReason());
    }

    private String cleanAiDescription(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        return description
                .split("\\s*\\[(강점|보완점|약점|리스크)]", 2)[0]
                .trim();
    }
}
//
