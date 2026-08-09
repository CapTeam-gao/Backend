package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.team.TeamMatchingVersionDiffResponseDto;
import com.capteam.gaobackend.dto.team.TeamMatchingVersionResponseDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationDetailResponseDto;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.*;
import com.capteam.gaobackend.repository.*;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamMatchingVersionService {

    // 버전 목록/상세/apply 대상 로드를 담당하는 Repository 필드입니다.
    private final TeamMatchingVersionRepository teamMatchingVersionRepository;

    // 버전 아래 추천안 목록을 읽고 상태를 바꾸는 Repository 필드입니다.
    private final TeamRecommendationRepository teamRecommendationRepository;

    // 추천안별 팀원 배정 정보로 diff와 실제 Team 생성까지 이어가기 위한 Repository 필드입니다.
    private final TeamRecommendationMemberRepository teamRecommendationMemberRepository;

    // 추천안별 이유 카드 조회에 사용하는 Repository 필드입니다.
    private final TeamRecommendationReasonRepository teamRecommendationReasonRepository;

    // 추천안 상세 응답에 학생 실력 레벨을 채우기 위한 Repository 필드입니다.
    private final UserAnalysisRepository userAnalysisRepository;

    // apply 시 기존 확정 팀을 교체하거나 새 팀을 만들기 위한 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // apply 시 실제 팀원 배정 row를 생성/삭제하기 위한 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // apply 시 기존 채팅방 정리와 새 채팅방 생성에 사용하는 Repository 필드입니다.
    private final ChatRoomRepository chatRoomRepository;

    // 팀 교체 전에 채널 정리와 기본 채널 생성에 사용하는 Repository 필드입니다.
    private final ChatChannelRepository chatChannelRepository;

    // 기존 채널에 메시지 이력이 있으면 위험한 교체를 막기 위해 확인하는 Repository 필드입니다.
    private final ChatMessageRepository chatMessageRepository;

    // 채널 삭제 전에 읽음 상태 row를 함께 정리하기 위한 Repository 필드입니다.
    private final ChatReadStatusRepository chatReadStatusRepository;

    // 팀 기획서가 있으면 팀 교체가 사용자 데이터를 잃게 되므로 검사에 사용하는 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

    // 일지 이력이 있는 팀은 롤백 시 기록을 깨뜨릴 수 있어 차단하기 위한 Repository 필드입니다.
    private final JournalRepository journalRepository;

    // 버전 적용 시 실제 확정 결과 공지까지 함께 최신화하기 위한 서비스 필드입니다.
    private final TeamAssignmentNoticeService teamAssignmentNoticeService;

    // 실제 Team 엔티티 한 팀에 들어갈 최대 인원 제한을 apply 단계에서도 동일하게 지킵니다.
    private static final int MAX_TEAM_MEMBER_COUNT = 5;

    // 특정 버전 아래의 추천안을 기존 상세 DTO shape 그대로 내려 프론트 재사용 범위를 넓힙니다.
    public List<TeamRecommendationDetailResponseDto> getVersionDetails(Long versionId) {
        // 먼저 버전 존재를 검증해 잘못된 id 요청을 조기에 막습니다.
        TeamMatchingVersion matchingVersion = getVersion(versionId);

        // 버전 안 추천안 순서를 고정해야 diff 화면과 apply 대상 확인이 일관됩니다.
        List<TeamRecommendation> recommendations =
                teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(matchingVersion.getId());

        return recommendations.stream()
                .map(this::toDetailResponse)
                .toList();
    }

    // 두 버전 사이에서 실제로 이동했거나 역할이 바뀐 학생만 골라 diff 응답을 구성합니다.
    public TeamMatchingVersionDiffResponseDto getVersionDiff(Long fromVersionId, Long toVersionId) {
        // 존재하지 않는 버전 id가 들어오면 빈 diff를 조용히 돌려주지 말고 바로 에러로 알립니다.
        // (getVersion()이 없으면 findByMatchingVersionIdOrderByIdAsc가 빈 리스트를 반환해
        //  "버전이 없음"과 "버전은 있는데 변경 없음"을 구분할 수 없게 됩니다.)
        getVersion(fromVersionId);
        getVersion(toVersionId);

        // 비교 시작 버전 상세를 먼저 펼쳐 두면 학생별 이전 위치를 빠르게 찾을 수 있습니다.
        Map<String, StudentPlacement> fromPlacements = buildPlacementMap(getVersionRecommendations(fromVersionId));

        // 비교 대상 버전 상세도 같은 구조로 만들어 두면 이동/역할 변화 판별이 단순해집니다.
        Map<String, StudentPlacement> toPlacements = buildPlacementMap(getVersionRecommendations(toVersionId));

        // 한쪽 버전에만 있는 학생까지 놓치지 않기 위해 userId 합집합 기준으로 순회합니다.
        Set<String> userIds = new LinkedHashSet<>();
        userIds.addAll(fromPlacements.keySet());
        userIds.addAll(toPlacements.keySet());

        // 화면에는 바뀐 학생만 보여줘야 관리자가 diff를 빠르게 읽을 수 있습니다.
        List<TeamMatchingVersionDiffResponseDto.MovedStudentDto> movedStudents = new ArrayList<>();

        // 강조 표시할 팀 id 집합도 함께 만들어 클라이언트의 추가 계산을 줄입니다.
        Set<String> changedTeamIds = new LinkedHashSet<>();

        for (String userId : userIds) {
            StudentPlacement fromPlacement = fromPlacements.get(userId);
            StudentPlacement toPlacement = toPlacements.get(userId);

            if (isSamePlacement(fromPlacement, toPlacement)) {
                continue;
            }

            if (fromPlacement != null && fromPlacement.getRecommendationId() != null) {
                changedTeamIds.add(String.valueOf(fromPlacement.getRecommendationId()));
            }
            if (toPlacement != null && toPlacement.getRecommendationId() != null) {
                changedTeamIds.add(String.valueOf(toPlacement.getRecommendationId()));
            }

            // 이름은 한쪽 버전에서라도 읽을 수 있으면 충분하므로 null이 아닌 쪽을 우선 사용합니다.
            String studentName = fromPlacement != null ? fromPlacement.getUserName() : toPlacement.getUserName();

            movedStudents.add(TeamMatchingVersionDiffResponseDto.MovedStudentDto.builder()
                    .userId(userId)
                    .name(studentName)
                    .fromTeamId(fromPlacement == null ? null : String.valueOf(fromPlacement.getRecommendationId()))
                    .fromTeamName(fromPlacement == null ? null : fromPlacement.getTeamName())
                    .toTeamId(toPlacement == null ? null : String.valueOf(toPlacement.getRecommendationId()))
                    .toTeamName(toPlacement == null ? null : toPlacement.getTeamName())
                    .fromStudentRole(fromPlacement == null ? null : fromPlacement.getStudentRole())
                    .toStudentRole(toPlacement == null ? null : toPlacement.getStudentRole())
                    .build());
        }

        return TeamMatchingVersionDiffResponseDto.builder()
                .movedStudents(movedStudents)
                .changedTeamIds(new ArrayList<>(changedTeamIds))
                .build();
    }

    // 선택한 버전을 실제 Team/TeamUser에 반영해 화면상의 초안과 운영 데이터를 맞춥니다.
    @Transactional
    public TeamMatchingVersionResponseDto applyVersion(Long versionId) {
        // 어떤 버전을 확정하는지 먼저 고정해 같은 트랜잭션 안에서 일관된 기준을 사용합니다.
        TeamMatchingVersion targetVersion = getVersion(versionId);

        // 실제 팀 생성에 필요한 추천안 묶음을 미리 읽어두어 중간 상태 없이 반영합니다.
        List<TeamRecommendation> recommendations = getVersionRecommendations(versionId);
        if (recommendations.isEmpty()) {
            throw new IllegalStateException("적용할 추천안이 없는 버전입니다.");
        }

        // 이미 확정된 학년 팀이 있다면 히스토리 손실 없이 교체 가능한지 먼저 검사합니다.
        List<Team> existingTeams = teamRepository.findByGrade(targetVersion.getGrade());
        validateGradeTeamReplaceable(existingTeams);

        // 교체 가능한 상태로 확인된 뒤에만 기존 팀 데이터를 제거해 중간 손실을 막습니다.
        deleteExistingGradeTeams(existingTeams);

        // 버전 추천안을 실제 Team/TeamUser로 다시 만들면 롤백도 동일 로직으로 처리할 수 있습니다.
        createTeamsFromVersion(targetVersion.getGrade(), recommendations);

        // 현재 적용된 버전 하나만 명확히 남기기 위해 같은 학년의 다른 버전은 폐기로 내립니다.
        discardOtherVersions(targetVersion);

        // 적용된 추천안이라는 의미를 상태에 남겨 이후 조회에서 버전 상태와 일치시킵니다.
        recommendations.forEach(TeamRecommendation::accept);
        targetVersion.apply();

        // 실제 팀 결과가 바뀌었으므로 팀 배정 공지도 최신 버전 기준으로 다시 맞춥니다.
        teamAssignmentNoticeService.createNotice(targetVersion.getGrade());

        return TeamMatchingVersionResponseDto.from(targetVersion);
    }

    // 아직 운영에 반영하지 않을 초안 버전을 목록에서 숨길 수 있게 폐기 상태로 바꿉니다.
    @Transactional
    public TeamMatchingVersionResponseDto discardVersion(Long versionId) {
        // 적용 중 버전까지 폐기하면 실제 운영 상태와 버전 메타데이터가 어긋나므로 막습니다.
        TeamMatchingVersion matchingVersion = getVersion(versionId);
        if (matchingVersion.getStatus() == TeamMatchingVersionStatus.APPLIED) {
            throw new IllegalStateException("현재 적용 중인 버전은 폐기할 수 없습니다.");
        }
        matchingVersion.discard();
        return TeamMatchingVersionResponseDto.from(matchingVersion);
    }

    // 특정 버전의 추천안들을 순서대로 읽어 상세 조회와 실제 apply 로직이 같은 기준을 쓰게 합니다.
    private List<TeamRecommendation> getVersionRecommendations(Long versionId) {
        return teamRecommendationRepository.findByMatchingVersionIdOrderByIdAsc(versionId);
    }

    // 버전 상세 응답은 기존 추천 상세 DTO를 그대로 재사용해 프론트 수정 범위를 줄입니다.
    private TeamRecommendationDetailResponseDto toDetailResponse(TeamRecommendation recommendation) {
        // 추천안 멤버 목록은 userId 기준 diff와 상세 카드 모두에 필요합니다.
        List<TeamRecommendationMember> members =
                teamRecommendationMemberRepository.findByRecommendationId(recommendation.getId());

        // 이유 카드는 기존 팀 추천 상세 화면에서 그대로 렌더링할 수 있습니다.
        List<TeamRecommendationReason> reasons =
                teamRecommendationReasonRepository.findByRecommendationId(recommendation.getId());

        // 멤버별 AI 분석 레벨을 별도 맵으로 준비해 DTO 변환 시 repository 재호출을 막습니다.
        Map<String, StudentLevel> levelMap = members.stream()
                .collect(Collectors.toMap(
                        member -> member.getUser().getUserId(),
                        member -> userAnalysisRepository.findById(member.getUser().getUserId())
                                .map(UserAnalysis::getStudentLevel)
                                .orElse(null)
                ));

        return TeamRecommendationDetailResponseDto.from(recommendation, members, reasons, levelMap);
    }

    // 학생별 소속 팀과 역할을 맵으로 만들면 두 버전 비교가 O(n)으로 단순해집니다.
    private Map<String, StudentPlacement> buildPlacementMap(List<TeamRecommendation> recommendations) {
        // 추천안 순서를 팀 이름으로 재사용해 버전 간 비교에서도 "1팀, 2팀"이 안정적으로 유지되게 합니다.
        Map<String, StudentPlacement> placements = new LinkedHashMap<>();

        for (int index = 0; index < recommendations.size(); index++) {
            // recommendation id와 순서 기반 팀 이름을 모두 보관해야 diff 화면에 링크와 텍스트를 함께 줄 수 있습니다.
            TeamRecommendation recommendation = recommendations.get(index);
            String teamName = (index + 1) + "팀";

            for (TeamRecommendationMember member : teamRecommendationMemberRepository.findByRecommendationId(recommendation.getId())) {
                placements.put(member.getUser().getUserId(), StudentPlacement.builder()
                        .recommendationId(recommendation.getId())
                        .teamName(teamName)
                        .userName(member.getUser().getName())
                        .studentRole(member.getStudentRole())
                        .build());
            }
        }

        return placements;
    }

    // 버그 이력: recommendationId(팀 추천안 row의 PK)로 비교했더니 재생성마다 모든 팀 row가
    // 새로 생성되어 "1팀→1팀"처럼 실제로는 안 바뀐 배정까지 전부 "이동"으로 잘못 표시됐다.
    // recommendationId는 버전마다 새로 발급되는 값이라 두 버전을 비교하는 동등성 기준으로 쓸 수 없다
    // (버전 간에 안정적으로 유지되는 값은 teamName뿐이다 — buildPlacementMap() 참고).
    // 따라서 "같은 배정"의 기준은 반드시 teamName + 역할로 판단해야 한다.
    private boolean isSamePlacement(StudentPlacement fromPlacement, StudentPlacement toPlacement) {
        if (fromPlacement == null && toPlacement == null) {
            return true;
        }
        if (fromPlacement == null || toPlacement == null) {
            return false;
        }
        return Objects.equals(fromPlacement.getTeamName(), toPlacement.getTeamName())
                && fromPlacement.getStudentRole() == toPlacement.getStudentRole();
    }

    // 이미 확정된 팀에 기록성 데이터가 있으면 무작정 교체하지 않고 명시적으로 막습니다.
    private void validateGradeTeamReplaceable(List<Team> existingTeams) {
        for (Team team : existingTeams) {
            // 팀 기획서는 사용자 입력 데이터라 apply/rollback 중 자동 삭제하면 안 됩니다.
            if (teamProjectRepository.findByTeamId(team.getId()).isPresent()) {
                throw new IllegalStateException("팀 기획서가 존재하는 학년 팀은 버전 전환할 수 없습니다.");
            }

            // 일지 데이터가 있으면 팀 id를 바꾸는 순간 히스토리가 끊어지므로 적용을 차단합니다.
            if (!journalRepository.findByTeamIdOrderByDateDesc(team.getId()).isEmpty()) {
                throw new IllegalStateException("일지 데이터가 존재하는 학년 팀은 버전 전환할 수 없습니다.");
            }

            Optional<ChatRoom> chatRoom = chatRoomRepository.findByTeamId(team.getId());
            if (chatRoom.isEmpty()) {
                continue;
            }

            // 채팅 메시지는 기록 데이터이므로 하나라도 있으면 자동 전환으로 삭제하지 않습니다.
            for (ChatChannel chatChannel : chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoom.get().getId())) {
                if (chatMessageRepository.findTopByChannelIdOrderByCreatedAtDesc(chatChannel.getId()).isPresent()) {
                    throw new IllegalStateException("채팅 메시지가 존재하는 학년 팀은 버전 전환할 수 없습니다.");
                }
            }
        }
    }

    // 교체 가능한 기존 팀에 대해서만 채팅/팀원/팀 row를 안전한 순서로 정리합니다.
    private void deleteExistingGradeTeams(List<Team> existingTeams) {
        for (Team team : existingTeams) {
            Optional<ChatRoom> chatRoom = chatRoomRepository.findByTeamId(team.getId());
            if (chatRoom.isPresent()) {
                // 읽음 상태와 메시지 row를 먼저 지워야 채널 삭제 시 FK 충돌이 나지 않습니다.
                for (ChatChannel chatChannel : chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoom.get().getId())) {
                    chatReadStatusRepository.deleteByChannelId(chatChannel.getId());
                    chatMessageRepository.deleteByChannelId(chatChannel.getId());
                    chatChannelRepository.delete(chatChannel);
                }
                chatRoomRepository.delete(chatRoom.get());
            }

            // 팀원을 먼저 비워야 팀 삭제 시 team_members FK 충돌을 피할 수 있습니다.
            teamUserRepository.deleteAll(teamUserRepository.findByTeamId(team.getId()));
            teamRepository.delete(team);
        }
    }

    // 추천 버전의 내용을 실제 Team/TeamUser 구조로 투영해 기존 화면과 기능이 그대로 동작하게 합니다.
    private void createTeamsFromVersion(Grade grade, List<TeamRecommendation> recommendations) {
        for (int index = 0; index < recommendations.size(); index++) {
            // 팀 이름은 apply 시점의 추천 순서 기준으로 다시 계산해 버전 간 결과가 안정적으로 보이게 합니다.
            TeamRecommendation recommendation = recommendations.get(index);
            List<TeamRecommendationMember> recommendedMembers =
                    teamRecommendationMemberRepository.findByRecommendationId(recommendation.getId());

            validateRecommendedMembers(recommendedMembers);

            // 실제 Team 엔티티는 추천 버전과 별개라 apply 시점에만 새로 만듭니다.
            Team team = teamRepository.save(Team.builder()
                    .teamName((index + 1) + "팀")
                    .grade(grade)
                    .status(TeamStatus.APPROVED)
                    .strengths(recommendation.getStrengths())
                    .weaknesses(recommendation.getWeaknesses())
                    .build());

            for (TeamRecommendationMember recommendedMember : recommendedMembers) {
                teamUserRepository.save(TeamUser.builder()
                        .team(team)
                        .user(recommendedMember.getUser())
                        .studentRole(recommendedMember.getStudentRole())
                        .leaderRole(recommendedMember.isRecommendedLeader() ? LeaderRole.LEADER : LeaderRole.MEMBER)
                        .build());
            }

            createTeamChatRoomWithDefaultChannel(team, recommendedMembers);
        }
    }

    // 추천 버전 데이터가 잘못 저장됐더라도 apply 직전에 한 번 더 인원/중복/설문 상태를 검증합니다.
    private void validateRecommendedMembers(List<TeamRecommendationMember> recommendedMembers) {
        if (recommendedMembers.isEmpty()) {
            throw new IllegalStateException("추천안에 팀원이 없습니다.");
        }
        if (recommendedMembers.size() > MAX_TEAM_MEMBER_COUNT) {
            throw new IllegalStateException("팀 인원은 최대 5명까지 가능합니다.");
        }

        // 같은 학생이 한 추천안 안에 두 번 들어가면 실제 team_members unique 제약에 걸리기 전에 차단합니다.
        Set<String> assignedUserIds = new LinkedHashSet<>();
        for (TeamRecommendationMember recommendedMember : recommendedMembers) {
            User user = recommendedMember.getUser();
            if (!user.isSurveyCompleted()) {
                throw new IllegalStateException("설문 미완료 학생이 있어 버전을 적용할 수 없습니다: " + user.getUserId());
            }
            if (!assignedUserIds.add(user.getUserId())) {
                throw new IllegalStateException("같은 학생이 추천안에 중복 포함되어 있습니다: " + user.getUserId());
            }
        }
    }

    // 채팅방과 기본 공통 채널을 다시 만들어야 버전 적용 직후 팀 채팅 기능이 바로 동작합니다.
    private void createTeamChatRoomWithDefaultChannel(Team team, List<TeamRecommendationMember> recommendedMembers) {
        // 추천 리더가 있으면 채널 생성자도 그 학생으로 맞춰 권한 표시를 자연스럽게 유지합니다.
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

    // 하나의 버전만 APPLIED로 남겨 화면 상태와 실제 운영 반영본을 일치시킵니다.
    private void discardOtherVersions(TeamMatchingVersion targetVersion) {
        for (TeamMatchingVersion version : teamMatchingVersionRepository.findByGradeOrderByVersionNumberDesc(targetVersion.getGrade())) {
            if (version.getId().equals(targetVersion.getId())) {
                continue;
            }
            version.discard();
        }
    }

    // 버전 id 검증을 공통화해 각 API 메서드가 동일한 예외 메시지를 사용하게 합니다.
    private TeamMatchingVersion getVersion(Long versionId) {
        return teamMatchingVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("팀 추천 버전을 찾을 수 없습니다."));
    }

    @Getter
    @Builder
    private static class StudentPlacement {

        // 추천안(TeamRecommendation row)의 PK. 버전마다 새로 생성되는 값이라 버전 간
        // 비교(동일 배정 여부 판단)에는 쓸 수 없다 — DTO의 fromTeamId/toTeamId(링크용)에만 사용할 것.
        private Long recommendationId;

        // 버전 간에도 안정적으로 유지되는 값 — "같은 배정인지" 비교는 반드시 이 필드로 한다.
        private String teamName;

        // userId만으로는 표가 읽기 어려워 이름을 함께 보관합니다.
        private String userName;

        // 역할 변경도 diff 대상이라 placement에 역할을 포함합니다.
        private StudentRole studentRole;
    }
}
