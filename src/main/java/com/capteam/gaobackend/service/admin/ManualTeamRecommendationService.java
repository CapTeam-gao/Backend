package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.team.ManualTeamRecommendationRequestDto;
import com.capteam.gaobackend.dto.team.TeamRecommendationResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManualTeamRecommendationService {

    private static final int MAX_TEAM_MEMBER_COUNT = 5;

    private final UserRepository userRepository;
    private final TeamUserRepository teamUserRepository;
    private final AdminTeamRecommendationPersistenceService recommendationPersistenceService;
    private final AdminTeamRecommendationService adminTeamRecommendationService;

    // 관리자가 직접 구성한 팀을 검증한 뒤 기존 추천안 저장 구조에 PENDING 추천안으로 저장하는 기능입니다.
    @Transactional
    public List<TeamRecommendationResponseDto> createManualRecommendations(ManualTeamRecommendationRequestDto dto) {
        validateRequiredFields(dto);

        Grade grade = dto.getGrade();
        List<User> targetStudents = findUnassignedGradeStudents(grade);
        if (targetStudents.isEmpty()) {
            throw new IllegalStateException("배정할 미배정 학생이 없습니다.");
        }

        Map<String, User> usersById = targetStudents.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));
        List<ManualTeamRecommendationRequestDto.ManualTeamDto> normalizedTeams = normalizeManualTeams(dto.getTeams(), usersById);
        validateManualTeams(normalizedTeams, usersById);

        return recommendationPersistenceService.replacePendingManualRecommendations(grade, normalizedTeams, usersById);
    }

    // 프론트 직접 구성 완료 API에서 사용합니다. 추천안 저장과 실제 팀 생성을 하나의 트랜잭션으로 처리합니다.
    @Transactional
    public void createAndAcceptManualTeams(ManualTeamRecommendationRequestDto dto) {
        List<TeamRecommendationResponseDto> recommendations = createManualRecommendations(dto);
        adminTeamRecommendationService.acceptRecommendations(
                recommendations.stream()
                        .map(TeamRecommendationResponseDto::getId)
                        .toList()
        );
    }

    private void validateRequiredFields(ManualTeamRecommendationRequestDto dto) {
        if (dto.getGrade() == null) {
            throw new IllegalArgumentException("학년을 선택해주세요.");
        }
        if (dto.getTeams() == null || dto.getTeams().isEmpty()) {
            throw new IllegalArgumentException("직접 구성할 팀을 1개 이상 입력해주세요.");
        }
    }

    private List<User> findUnassignedGradeStudents(Grade grade) {
        Set<String> assignedUserIds = teamUserRepository.findAll().stream()
                .map(teamUser -> teamUser.getUser().getUserId())
                .collect(Collectors.toSet());

        return userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, grade)
                .stream()
                .filter(user -> !assignedUserIds.contains(user.getUserId()))
                .toList();
    }

    private List<ManualTeamRecommendationRequestDto.ManualTeamDto> normalizeManualTeams(
            List<ManualTeamRecommendationRequestDto.ManualTeamDto> teams,
            Map<String, User> usersById
    ) {
        List<ManualTeamRecommendationRequestDto.ManualTeamDto> normalizedTeams = new ArrayList<>();
        int fallbackTeamNumber = 1;

        for (ManualTeamRecommendationRequestDto.ManualTeamDto team : teams) {
            Integer teamNumber = resolveTeamNumber(team, fallbackTeamNumber);
            List<ManualTeamRecommendationRequestDto.ManualTeamMemberDto> members = normalizeMembers(team, usersById);
            normalizedTeams.add(new ManualTeamRecommendationRequestDto.ManualTeamDto(teamNumber, members));
            fallbackTeamNumber++;
        }

        return normalizedTeams;
    }

    private Integer resolveTeamNumber(ManualTeamRecommendationRequestDto.ManualTeamDto team, int fallbackTeamNumber) {
        if (team.getTeamNumber() != null) {
            return team.getTeamNumber();
        }
        if (team.getTeamName() != null) {
            String digits = team.getTeamName().replaceAll("[^0-9]", "");
            if (!digits.isBlank()) {
                return Integer.parseInt(digits);
            }
        }
        return fallbackTeamNumber;
    }

    private List<ManualTeamRecommendationRequestDto.ManualTeamMemberDto> normalizeMembers(
            ManualTeamRecommendationRequestDto.ManualTeamDto team,
            Map<String, User> usersById
    ) {
        List<ManualTeamRecommendationRequestDto.ManualTeamMemberDto> members = team.getMembers();
        if (members != null && !members.isEmpty()) {
            return members;
        }

        List<String> memberUserIds = team.getMemberUserIds();
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            return List.of();
        }

        return memberUserIds.stream()
                .map(userId -> {
                    String trimmedUserId = userId == null ? "" : userId.trim();
                    User user = usersById.get(trimmedUserId);
                    StudentRole role = user == null ? null : user.getStudentRole();
                    boolean leader = trimmedUserId.equals(team.getLeaderUserId());
                    return new ManualTeamRecommendationRequestDto.ManualTeamMemberDto(trimmedUserId, role, leader);
                })
                .toList();
    }

    private void validateManualTeams(
            List<ManualTeamRecommendationRequestDto.ManualTeamDto> teams,
            Map<String, User> usersById
    ) {
        Set<Integer> teamNumbers = new LinkedHashSet<>();
        Set<String> assignedUserIds = new LinkedHashSet<>();
        List<String> duplicateUserIds = new ArrayList<>();

        for (ManualTeamRecommendationRequestDto.ManualTeamDto team : teams) {
            validateTeam(team, teamNumbers, assignedUserIds, duplicateUserIds, usersById);
        }

        if (!duplicateUserIds.isEmpty()) {
            throw new IllegalArgumentException("한 학생은 여러 팀에 중복 배정될 수 없습니다: " + String.join(", ", duplicateUserIds));
        }

        // 프론트 직접 구성 화면은 미배정 학생이 남아도 확인 후 진행할 수 있으므로,
        // 여기서는 "배정된 학생이 유효한지"만 검증합니다.
    }

    private void validateTeam(
            ManualTeamRecommendationRequestDto.ManualTeamDto team,
            Set<Integer> teamNumbers,
            Set<String> assignedUserIds,
            List<String> duplicateUserIds,
            Map<String, User> usersById
    ) {
        if (team.getTeamNumber() == null || team.getTeamNumber() <= 0) {
            throw new IllegalArgumentException("팀 번호는 1 이상이어야 합니다.");
        }
        if (!teamNumbers.add(team.getTeamNumber())) {
            throw new IllegalArgumentException("팀 번호가 중복되었습니다: " + team.getTeamNumber());
        }

        List<ManualTeamRecommendationRequestDto.ManualTeamMemberDto> members = safeMembers(team);
        if (members.isEmpty()) {
            throw new IllegalArgumentException(team.getTeamNumber() + "팀에 학생을 1명 이상 배정해주세요.");
        }
        if (members.size() > MAX_TEAM_MEMBER_COUNT) {
            throw new IllegalArgumentException("한 팀은 최대 5명까지 배정할 수 있습니다: " + team.getTeamNumber() + "팀");
        }

        long leaderCount = members.stream()
                .filter(ManualTeamRecommendationRequestDto.ManualTeamMemberDto::isLeader)
                .count();
        if (leaderCount > 1) {
            throw new IllegalArgumentException("한 팀에는 팀장을 1명만 지정할 수 있습니다: " + team.getTeamNumber() + "팀");
        }

        for (ManualTeamRecommendationRequestDto.ManualTeamMemberDto member : members) {
            validateMember(member, assignedUserIds, duplicateUserIds, usersById);
        }
    }

    private List<ManualTeamRecommendationRequestDto.ManualTeamMemberDto> safeMembers(
            ManualTeamRecommendationRequestDto.ManualTeamDto team
    ) {
        return team.getMembers() == null ? List.of() : team.getMembers();
    }

    private void validateMember(
            ManualTeamRecommendationRequestDto.ManualTeamMemberDto member,
            Set<String> assignedUserIds,
            List<String> duplicateUserIds,
            Map<String, User> usersById
    ) {
        if (member.getUserId() == null || member.getUserId().isBlank()) {
            throw new IllegalArgumentException("팀원 userId를 입력해주세요.");
        }
        String userId = member.getUserId().trim();
        if (member.getRole() == null) {
            throw new IllegalArgumentException("팀원 역할을 선택해주세요: " + userId);
        }
        if (!usersById.containsKey(userId)) {
            throw new IllegalArgumentException("직접 구성 대상 학생을 찾을 수 없습니다: " + userId);
        }
        if (!assignedUserIds.add(userId)) {
            duplicateUserIds.add(userId);
        }
    }
}
