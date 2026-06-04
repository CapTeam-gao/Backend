package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminStudentDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListPageResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListResponseDto;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStudentService {

    // 관리자 학생 목록/상세 조회에 필요한 팀원 정보를 조회하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 학생별 AI 분석 결과를 함께 보여주기 위한 Repository 필드입니다.
    private final UserAnalysisRepository userAnalysisRepository;

    // 관리자가 전체 학생 통계와 검색 조건이 반영된 학생 목록을 AI 분석 정보와 함께 조회하는 기능입니다.
    public AdminStudentListPageResponseDto getAllStudents(String name, String userId, StudentRole studentRole) {

        List<TeamUser> teamUsers = teamUserRepository.findAll();
        Map<String, UserAnalysis> userAnalysisMap = userAnalysisRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        UserAnalysis::getUserId,
                        userAnalysis -> userAnalysis
                ));

        List<AdminStudentListResponseDto> filteredStudents = teamUsers.stream()
                .filter(teamUser -> matchesName(teamUser, name))
                .filter(teamUser -> matchesUserId(teamUser, userId))
                .filter(teamUser -> matchesStudentRole(teamUser, studentRole))
                .map(teamUser -> AdminStudentListResponseDto.from(
                        teamUser,
                        userAnalysisMap.get(teamUser.getUser().getUserId())
                ))
                .toList();

        return AdminStudentListPageResponseDto.of(teamUsers, filteredStudents);
    }


    // 관리자가 특정 학생 상세 정보를 조회하는 기능입니다.
    public AdminStudentDetailResponseDto getStudentDetail(String userId) {
        TeamUser teamUser = teamUserRepository.findByUserUserId(userId)
                .orElseThrow(UserNotFoundException::new);

        UserAnalysis userAnalysis = userAnalysisRepository.findByUserUserId(userId)
                .orElse(null);

        return AdminStudentDetailResponseDto.from(teamUser, userAnalysis);
    }

    // 이름 검색어가 비어 있으면 전체 허용하고, 값이 있으면 학생 이름에 포함되는지 확인하는 기능입니다.
    private boolean matchesName(TeamUser teamUser, String name) {
        return isBlank(name) || teamUser.getUser().getName().contains(name.trim());
    }

    // 학번 검색어가 비어 있으면 전체 허용하고, 값이 있으면 userId에 포함되는지 확인하는 기능입니다.
    private boolean matchesUserId(TeamUser teamUser, String userId) {
        return isBlank(userId) || teamUser.getUser().getUserId().contains(userId.trim());
    }

    // 희망 직군 검색 조건이 없으면 전체 허용하고, 값이 있으면 학생 역할과 일치하는지 확인하는 기능입니다.
    private boolean matchesStudentRole(TeamUser teamUser, StudentRole studentRole) {
        return studentRole == null || teamUser.getStudentRole() == studentRole;
    }

    // 문자열이 null이거나 공백인지 확인하는 기능입니다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
