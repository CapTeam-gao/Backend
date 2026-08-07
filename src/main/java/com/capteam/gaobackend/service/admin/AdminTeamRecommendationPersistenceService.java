package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.dto.team.ManualTeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.TeamMatchingVersion;
import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.entity.TeamRecommendationMember;
import com.capteam.gaobackend.entity.TeamRecommendationReason;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.repository.TeamMatchingVersionRepository;
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

    // 새 추천안을 어떤 버전 묶음에 저장할지 발급하는 Repository 필드입니다.
    private final TeamMatchingVersionRepository teamMatchingVersionRepository;

    // 버전 아래 실제 추천안 row를 저장하는 Repository 필드입니다.
    private final TeamRecommendationRepository recommendationRepository;

    // 추천안별 멤버 배정 정보를 저장하는 Repository 필드입니다.
    private final TeamRecommendationMemberRepository recommendationMemberRepository;

    // 추천안 설명 카드 저장에 사용하는 Repository 필드입니다.
    private final TeamRecommendationReasonRepository recommendationReasonRepository;

    // AI 결과의 userId/name을 실제 학생 엔티티로 연결하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 취소 검증을 통과한 AI 결과를 새로운 DRAFT 버전으로 저장합니다.
    @Transactional
    public List<TeamRecommendationResponseDto> replacePendingRecommendations(
            Grade grade,
            Map<String, String> nameToUserId,
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams
    ) {
        return replacePendingRecommendations(grade, nameToUserId, targetTeams, null, null);
    }

    // 비동기 작업과 재생성 프롬프트를 버전 메타데이터에 남기기 위한 저장 진입점입니다.
    @Transactional
    public List<TeamRecommendationResponseDto> replacePendingRecommendations(
            Grade grade,
            Map<String, String> nameToUserId,
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams,
            String jobId,
            String regenerationPrompt
    ) {
        // 같은 학년 안에서 단조 증가하는 번호를 써야 버전 목록이 사람이 읽기 쉽습니다.
        TeamMatchingVersion matchingVersion = createDraftVersion(grade, jobId, regenerationPrompt);

        // AI 응답에는 userId 또는 이름이 올 수 있으므로 저장 전에 실학생 엔티티 맵을 고정합니다.
        Map<String, User> usersById = userRepository.findAllById(nameToUserId.values()).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        // 저장 결과를 바로 응답으로 돌려주기 위해 생성한 추천안 목록을 모읍니다.
        List<TeamRecommendationResponseDto> result = new ArrayList<>();
        for (AiTeamSummaryResponseDto.TeamDto aiTeam : targetTeams) {
            // 다른 학년 학생이 섞인 AI 결과는 현재 학년 사용자로 식별된 멤버만 저장합니다.
            List<AiTeamSummaryResponseDto.MemberDto> validMembers = aiTeam.getMembers().stream()
                    .filter(member -> AiTeamMemberUserResolver.resolveUserId(member, nameToUserId) != null)
                    .toList();

            if (validMembers.isEmpty()) {
                continue;
            }

            TeamRecommendation recommendation = recommendationRepository.save(
                    TeamRecommendation.builder()
                            .matchingVersion(matchingVersion)
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

    // 관리자가 직접 구성한 팀도 동일한 버전 단위로 저장해 나중에 apply/discard할 수 있게 합니다.
    @Transactional
    public List<TeamRecommendationResponseDto> replacePendingManualRecommendations(
            Grade grade,
            List<ManualTeamRecommendationRequestDto.ManualTeamDto> teams,
            Map<String, User> usersById
    ) {
        // 수동 구성도 AI 생성과 동일하게 새 버전 번호를 발급해 비교 대상이 남도록 합니다.
        TeamMatchingVersion matchingVersion = createDraftVersion(grade, null, "MANUAL");

        // 즉시 화면에 렌더링할 추천안 목록을 반환하기 위해 저장 결과를 모읍니다.
        List<TeamRecommendationResponseDto> result = new ArrayList<>();
        for (ManualTeamRecommendationRequestDto.ManualTeamDto manualTeam : teams) {
            TeamRecommendation recommendation = recommendationRepository.save(
                    TeamRecommendation.builder()
                            .matchingVersion(matchingVersion)
                            .grade(grade)
                            .strengths("관리자가 직접 구성한 팀입니다.")
                            .weaknesses(null)
                            .build()
            );

            for (ManualTeamRecommendationRequestDto.ManualTeamMemberDto member : manualTeam.getMembers()) {
                User user = usersById.get(member.getUserId().trim());
                if (user == null) {
                    throw new IllegalStateException("직접 구성 대상 학생을 찾을 수 없습니다: " + member.getUserId());
                }
                recommendationMemberRepository.save(TeamRecommendationMember.builder()
                        .recommendation(recommendation)
                        .user(user)
                        .studentRole(member.getRole())
                        .isRecommendedLeader(member.isLeader())
                        .build());
            }

            recommendationReasonRepository.save(TeamRecommendationReason.builder()
                    .recommendation(recommendation)
                    .title("직접 구성")
                    .description(manualTeam.getTeamNumber() + "팀은 관리자가 직접 구성한 추천안입니다.")
                    .build());
            result.add(TeamRecommendationResponseDto.from(recommendation));
        }

        return result;
    }

    // 버전 번호는 학년별 최신 번호 + 1 규칙으로 발급해 사람이 봐도 순서를 추적할 수 있게 합니다.
    private TeamMatchingVersion createDraftVersion(Grade grade, String jobId, String regenerationPrompt) {
        Integer nextVersionNumber = teamMatchingVersionRepository.findFirstByGradeOrderByVersionNumberDesc(grade)
                .map(TeamMatchingVersion::getVersionNumber)
                .map(versionNumber -> versionNumber + 1)
                .orElse(1);

        return teamMatchingVersionRepository.save(TeamMatchingVersion.builder()
                .grade(grade)
                .versionNumber(nextVersionNumber)
                .jobId(jobId)
                .regenerationPrompt(regenerationPrompt)
                .build());
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
