package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.team.PreferredTeammateRequestDto;
import com.capteam.gaobackend.dto.team.PreferredTeammateResponseDto;
import com.capteam.gaobackend.dto.team.MyTeamResponseDto;
import com.capteam.gaobackend.dto.team.TeamDetailResponseDto;
import com.capteam.gaobackend.dto.team.TeamMemberTaskRequestDto;
import com.capteam.gaobackend.dto.team.TeamMemberUpdateRequestDto;
import com.capteam.gaobackend.dto.team.TeamProjectRequestDto;
import com.capteam.gaobackend.dto.team.TeamSummaryResponseDto;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    // 선호 팀원 userId 검증과 사용자 이름 확인에 사용하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 관리자 팀원 이동 시 대상 팀을 조회하는 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 학생이 소속된 팀원 정보를 조회하거나 수정하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 팀 프로젝트 기획서를 조회하거나 저장하는 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

    // 사용자가 등록한 선호 팀원 목록을 상세 정보로 조회하는 기능입니다.
    public PreferredTeammateResponseDto getPreferences(String userId) {
        User user = findUser(userId);
        List<String> preferredIds = user.getPreferredTeammates() == null ? List.of() : user.getPreferredTeammates();

        return PreferredTeammateResponseDto.builder()
                .preferredTeammates(preferredIds.stream()
                        .map(this::findUser)
                        .map(PreferredTeammateResponseDto.TeammateDto::from)
                        .toList())
                .build();
    }

    // 선호 팀원 목록을 최대 3명까지 검증하고 사용자 프로필에 저장하는 기능입니다.
    @Transactional
    public PreferredTeammateResponseDto updatePreferences(String userId, PreferredTeammateRequestDto request) {
        User user = findUser(userId);
        List<String> teammates = request.getPreferredTeammates();

        if (teammates == null) {
            teammates = List.of();
        }
        if (teammates.size() > 3) {
            throw new IllegalArgumentException("선호 팀원은 최대 3명까지 등록할 수 있습니다.");
        }

        Set<String> preferredIds = new LinkedHashSet<>();
        List<String> resolvedIds = new ArrayList<>();
        for (String teammateUserId : teammates) {
            String normalizedUserId = teammateUserId == null ? null : teammateUserId.trim();
            if (normalizedUserId == null || normalizedUserId.isEmpty()) {
                throw new IllegalArgumentException("선호 팀원 userId는 비어 있을 수 없습니다.");
            }
            if (user.getUserId().equals(normalizedUserId)) {
                throw new IllegalArgumentException("본인은 선호 팀원으로 등록할 수 없습니다.");
            }
            if (!preferredIds.add(normalizedUserId)) {
                throw new IllegalArgumentException("같은 학생을 선호 팀원으로 중복 등록할 수 없습니다.");
            }

            User preferredUser = findUser(normalizedUserId);
            validatePreferredStudent(user, preferredUser);
            resolvedIds.add(preferredUser.getUserId());
        }

        user.updateProfile(
                user.getStudentRole(),
                user.getSkill(),
                user.getExperience(),
                user.isWantsLeader(),
                List.copyOf(resolvedIds)
        );

        return getPreferences(userId);
    }

    // 로그인한 학생의 소속 팀 상세 정보를 조회하는 기능입니다.
    public MyTeamResponseDto getMyTeam(String userId) {
        TeamUser myTeamUser = findMyTeamUser(userId);
        Team team = myTeamUser.getTeam();
        TeamProject teamProject = teamProjectRepository.findByTeamId(team.getId()).orElse(null);
        List<TeamUser> teamUsers = teamUserRepository.findByTeamId(team.getId());

        return MyTeamResponseDto.from(team, teamProject, myTeamUser, teamUsers);
    }

    // 로그인한 학생이 소속된 팀의 요약 정보를 조회하는 기능입니다.
    public TeamSummaryResponseDto getMyTeamSummary(String userId) {
        TeamUser myTeamUser = findMyTeamUser(userId);
        Team team = myTeamUser.getTeam();
        TeamProject teamProject = teamProjectRepository.findByTeamId(team.getId()).orElse(null);
        List<TeamUser> teamUsers = teamUserRepository.findByTeamId(team.getId());

        return TeamSummaryResponseDto.from(team, teamProject, teamUsers);
    }

    // 로그인한 학생이 소속된 특정 팀의 상세 정보를 조회하는 기능입니다.
    public TeamDetailResponseDto getTeamDetail(String userId, Long teamId) {
        TeamUser myTeamUser = findMyTeamUser(userId);
        if (!myTeamUser.getTeam().getId().equals(teamId)) {
            throw new IllegalArgumentException("본인이 소속된 팀만 조회할 수 있습니다.");
        }

        Team team = myTeamUser.getTeam();
        TeamProject teamProject = teamProjectRepository.findByTeamId(team.getId()).orElse(null);
        List<TeamUser> teamUsers = teamUserRepository.findByTeamId(team.getId());

        return TeamDetailResponseDto.from(team, teamProject, teamUsers);
    }

    // 로그인한 학생이 소속된 팀의 프로젝트 기획서를 조회하는 기능입니다.
    public MyTeamResponseDto.TeamProjectDto getMyTeamProject(String userId) {
        TeamUser myTeamUser = teamUserRepository.findByUserUserId(userId).orElse(null);
        if (myTeamUser == null) {
            return null;
        }

        return teamProjectRepository.findByTeamId(myTeamUser.getTeam().getId())
                .map(MyTeamResponseDto.TeamProjectDto::from)
                .orElse(null);
    }

    // 로그인한 학생이 소속된 팀의 프로젝트 기획서를 생성하거나 수정하는 기능입니다.
    @Transactional
    public MyTeamResponseDto.TeamProjectDto upsertMyTeamProject(String userId, TeamProjectRequestDto request) {
        TeamUser myTeamUser = findMyTeamUser(userId);
        Team team = myTeamUser.getTeam();

        String teamName = normalizeRequiredText(request.getTeamName(), "팀명");
        String serviceName = normalizeRequiredText(request.getServiceName(), "서비스명");
        String serviceIntro = normalizeRequiredText(request.getServiceIntro(), "서비스 소개");
        String mainFeatures = normalizeRequiredText(request.getMainFeatures(), "주요 기능");

        TeamProject teamProject = teamProjectRepository.findByTeamId(team.getId())
                .map(existingProject -> {
                    existingProject.update(teamName, serviceName, serviceIntro, mainFeatures);
                    return existingProject;
                })
                .orElseGet(() -> teamProjectRepository.save(TeamProject.builder()
                        .team(team)
                        .teamName(teamName)
                        .serviceName(serviceName)
                        .serviceIntro(serviceIntro)
                        .mainFeatures(mainFeatures)
                        .build()));

        return MyTeamResponseDto.TeamProjectDto.from(teamProject);
    }

    // 관리자가 특정 학생을 다른 팀으로 이동시키고 역할/팀장 여부를 수정하는 기능입니다.
    @Transactional
    public void updateTeamMember(TeamMemberUpdateRequestDto request) {
        TeamUser teamUser = teamUserRepository.findByUserUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("팀원을 찾을 수 없습니다."));
        Team targetTeam = teamRepository.findById(request.getTargetTeamId())
                .orElseThrow(() -> new IllegalArgumentException("이동할 팀을 찾을 수 없습니다."));

        teamUser.updateTeamMember(
                targetTeam,
                request.getStudentRole(),
                request.getLeaderRole()
        );
    }

    // 팀원이 본인의 담당 업무를 수정하는 기능입니다. 팀장을 포함해 본인 것만 수정할 수 있습니다.
    @Transactional
    public void updateAssignedTask(String requesterId, String targetUserId, TeamMemberTaskRequestDto request) {
        if (!requesterId.equals(targetUserId)) {
            throw new AccessDeniedException("본인의 담당 업무만 수정할 수 있습니다.");
        }

        TeamUser teamUser = teamUserRepository.findByUserUserId(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("소속된 팀이 없습니다."));

        String assignedTask = request.getAssignedTask() == null ? null : request.getAssignedTask().trim();
        teamUser.updateAssignedTask(assignedTask == null || assignedTask.isEmpty() ? null : assignedTask);
    }

    // userId로 사용자를 조회하고 없으면 예외를 발생시키는 기능입니다.
    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    // 선호 팀원이 같은 학년의 학생 계정인지 검증하는 기능입니다.
    private void validatePreferredStudent(User user, User preferredUser) {
        if (preferredUser.getAccountRole() != AccountRole.STUDENT) {
            throw new IllegalArgumentException("학생 계정만 선호 팀원으로 등록할 수 있습니다.");
        }
        if (user.getGrade() != null && preferredUser.getGrade() != user.getGrade()) {
            throw new IllegalArgumentException("같은 학년 학생만 선호 팀원으로 등록할 수 있습니다.");
        }
    }

    // 로그인한 학생의 TeamUser 정보를 조회하고 팀 미배정이면 예외를 발생시키는 기능입니다.
    private TeamUser findMyTeamUser(String userId) {
        findUser(userId);

        return teamUserRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("아직 소속된 팀이 없습니다."));
    }

    // 필수 문자열 입력값의 앞뒤 공백을 제거하고 비어 있으면 예외를 발생시키는 기능입니다.
    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        return value.trim();
    }
}
