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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    // AI가 최종 결과를 보내는 지점이라, 같은 jobId로 배치 스트리밍이 이미 만들어둔 버전이
    // 있으면 그 버전을 재사용하고 임시로 쌓인 배치 결과는 비운 뒤 최종 결과로 다시 채웁니다.
    // (재사용/정리를 안 하면 같은 job에 대해 버전이 두 개 생기고 팀도 중복으로 남습니다.)
    @Transactional
    public List<TeamRecommendationResponseDto> replacePendingRecommendations(
            Grade grade,
            Map<String, String> nameToUserId,
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams,
            String jobId,
            String regenerationPrompt
    ) {
        TeamMatchingVersion matchingVersion = findVersionByJobId(jobId)
                .orElseGet(() -> createDraftVersion(grade, jobId, regenerationPrompt));

        clearRecommendations(matchingVersion);

        return saveTeams(matchingVersion, grade, nameToUserId, targetTeams, false);
    }

    // 배치 스트리밍 콜백으로 도착한 팀 일부를, 이미 진행 중인 job의 버전에 이어서 저장합니다.
    // (동일 jobId로 처음 오는 배치면 버전을 새로 만들고, 이후 배치는 같은 버전에 계속 추가합니다.)
    // 같은 팀이 team_update → team_ready처럼 여러 번 도착할 수 있어 upsert(true)로 저장합니다.
    @Transactional
    public List<TeamRecommendationResponseDto> appendBatchTeams(
            Grade grade,
            String jobId,
            String regenerationPrompt,
            Map<String, String> nameToUserId,
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams
    ) {
        TeamMatchingVersion matchingVersion = findVersionByJobId(jobId)
                .orElseGet(() -> createDraftVersion(grade, jobId, regenerationPrompt));

        return saveTeams(matchingVersion, grade, nameToUserId, targetTeams, true);
    }

    // jobId가 없는 호출(수동 구성 등)까지 findByJobId(null)로 잘못 매칭되지 않도록 null을 먼저 걸러냅니다.
    private Optional<TeamMatchingVersion> findVersionByJobId(String jobId) {
        if (jobId == null) {
            return Optional.empty();
        }
        return teamMatchingVersionRepository.findByJobId(jobId);
    }

    // 버전에 이미 저장된 추천안(배치 스트리밍 임시 결과)을 전부 지웁니다.
    // 최종 결과로 완전히 교체하는 시점(replacePendingRecommendations)에만 사용합니다.
    private void clearRecommendations(TeamMatchingVersion matchingVersion) {
        List<TeamRecommendation> existing = recommendationRepository.findByMatchingVersionId(matchingVersion.getId());
        for (TeamRecommendation recommendation : existing) {
            recommendationMemberRepository.deleteByRecommendationId(recommendation.getId());
            recommendationReasonRepository.deleteByRecommendationId(recommendation.getId());
        }
        recommendationRepository.deleteAll(existing);
    }

    // AI 팀 목록을 주어진 버전 아래에 저장하는 공통 로직입니다. 전체 교체(replacePendingRecommendations)와
    // 배치 이어붙이기(appendBatchTeams) 모두 이 메서드로 실제 저장을 수행합니다.
    // upsertByMembers=true면, 이번에 들어온 학생과 한 명이라도 겹치는 기존 팀을 통째로 지우고
    // 다시 만듭니다(팀이 새로 재편됐을 수 있으니 부분 수정 대신 재구성). false면(최종 저장 직전에
    // clearRecommendations로 이미 비워진 상태) 겹치는 팀이 없으니 항상 새로 만듭니다.
    //
    // 버그 이력: 이전엔 AI가 응답에 실어 보내는 team_name("1팀" 등) 문자열로 같은 팀인지
    // 판단했는데, 이 이름은 배치 호출마다(team_update → team_ready) 안정적으로 유지된다는
    // 보장이 없어서(AI가 같은 학생들을 다른 이름의 팀으로 다시 보고하면) 실제로는 갱신돼야
    // 할 팀이 새 팀으로 또 추가되어, 같은 학생이 여러 팀에 중복으로 남는 사고가 실제로 발생했다.
    // 학생 userId는 배치 호출과 무관하게 항상 안정적이므로, "같은 팀"인지는 이름이 아니라
    // 실제로 겹치는 학생이 있는지로 판단해야 한다.
    private List<TeamRecommendationResponseDto> saveTeams(
            TeamMatchingVersion matchingVersion,
            Grade grade,
            Map<String, String> nameToUserId,
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams,
            boolean upsertByMembers
    ) {
        // AI 응답에는 userId 또는 이름이 올 수 있으므로 저장 전에 실학생 엔티티 맵을 고정합니다.
        Map<String, User> usersById = userRepository.findAllById(nameToUserId.values()).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        validateTargetTeams(targetTeams, nameToUserId);

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

            if (upsertByMembers) {
                Set<String> incomingUserIds = validMembers.stream()
                        .map(member -> AiTeamMemberUserResolver.resolveUserId(member, nameToUserId))
                        .collect(Collectors.toSet());
                removeTeamsSharingMembers(matchingVersion, incomingUserIds);
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

    // 한 번의 AI 배치 안에서 같은 학생이 여러 팀에 들어간 결과는 저장하지 않습니다.
    // 배치가 다음 단계에서 같은 팀을 갱신하는 경우는 기존 팀을 upsert하므로 허용합니다.
    private void validateTargetTeams(
            List<AiTeamSummaryResponseDto.TeamDto> targetTeams,
            Map<String, String> nameToUserId
    ) {
        Set<String> assignedUserIds = new java.util.HashSet<>();
        for (AiTeamSummaryResponseDto.TeamDto aiTeam : targetTeams) {
            for (AiTeamSummaryResponseDto.MemberDto member : aiTeam.getMembers()) {
                String userId = AiTeamMemberUserResolver.resolveUserId(member, nameToUserId);
                if (userId != null && !assignedUserIds.add(userId)) {
                    throw new IllegalStateException(
                            "AI 추천 결과에 같은 학생이 여러 팀으로 배정되었습니다: " + userId
                    );
                }
            }
        }
    }

    // 이번 배치에 새로 들어온 학생과 한 명이라도 겹치는 기존 팀을 통째로 지웁니다.
    private void removeTeamsSharingMembers(TeamMatchingVersion matchingVersion, Set<String> incomingUserIds) {
        List<TeamRecommendation> existing = recommendationRepository.findByMatchingVersionId(matchingVersion.getId());
        for (TeamRecommendation candidate : existing) {
            Set<String> candidateUserIds = recommendationMemberRepository.findByRecommendationId(candidate.getId()).stream()
                    .map(member -> member.getUser().getUserId())
                    .collect(Collectors.toSet());

            if (!Collections.disjoint(candidateUserIds, incomingUserIds)) {
                recommendationMemberRepository.deleteByRecommendationId(candidate.getId());
                recommendationReasonRepository.deleteByRecommendationId(candidate.getId());
                recommendationRepository.delete(candidate);
            }
        }
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
