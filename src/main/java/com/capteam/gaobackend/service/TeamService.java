package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.team.PreferredTeammateRequestDto;
import com.capteam.gaobackend.dto.team.PreferredTeammateResponseDto;
import com.capteam.gaobackend.dto.team.TeamMemberUpdateRequestDto;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        List<PreferredTeammateRequestDto.PreferredTeammateDto> teammates = request.getPreferredTeammates();

        if (teammates == null) {
            teammates = List.of();
        }
        if (teammates.size() > 3) {
            throw new IllegalArgumentException("선호 팀원은 최대 3명까지 등록할 수 있습니다.");
        }

        Set<String> preferredIds = new LinkedHashSet<>();
        for (PreferredTeammateRequestDto.PreferredTeammateDto teammate : teammates) {
            if (user.getUserId().equals(teammate.getUserId())) {
                throw new IllegalArgumentException("본인은 선호 팀원으로 등록할 수 없습니다.");
            }

            User preferredUser = findUser(teammate.getUserId());
            if (!preferredUser.getName().equals(teammate.getName())) {
                throw new IllegalArgumentException("학번과 이름이 일치하지 않습니다: " + teammate.getUserId());
            }
            preferredIds.add(preferredUser.getUserId());
        }

        user.updateProfile(
                user.getStudentRole(),
                user.getSkill(),
                user.getExperience(),
                user.isWantsLeader(),
                List.copyOf(preferredIds)
        );

        return getPreferences(userId);
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

    // userId로 사용자를 조회하고 없으면 예외를 발생시키는 기능입니다.
    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}
