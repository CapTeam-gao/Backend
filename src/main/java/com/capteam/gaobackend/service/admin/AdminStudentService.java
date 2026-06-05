package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminStudentDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListPageResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListResponseDto;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import com.capteam.gaobackend.repository.UserRepository;
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

    // 팀 배정 전 학생까지 목록에 포함하기 위해 사용자 정보를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 관리자가 전체 학생 통계와 검색 조건이 반영된 학생 목록을 AI 분석 정보와 함께 조회하는 기능입니다.
    public AdminStudentListPageResponseDto getAllStudents(
            String name,
            String userId,
            StudentRole studentRole,
            Grade grade,
            Boolean surveyCompleted
    ) {

        List<User> students = userRepository.findAll().stream()
                .filter(user -> user.getAccountRole() == AccountRole.STUDENT)
                .toList();
        Map<String, TeamUser> teamUserMap = teamUserRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        teamUser -> teamUser.getUser().getUserId(),
                        teamUser -> teamUser,
                        (first, second) -> first
                ));
        Map<String, UserAnalysis> userAnalysisMap = userAnalysisRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        UserAnalysis::getUserId,
                        userAnalysis -> userAnalysis
                ));

        List<AdminStudentListResponseDto> filteredStudents = students.stream()
                .filter(user -> matchesName(user, name))
                .filter(user -> matchesUserId(user, userId))
                .filter(user -> matchesGrade(user, grade))
                .filter(user -> matchesSurveyCompleted(user, surveyCompleted))
                .filter(user -> matchesStudentRole(user, teamUserMap.get(user.getUserId()), studentRole))
                .map(user -> AdminStudentListResponseDto.from(
                        user,
                        teamUserMap.get(user.getUserId()),
                        userAnalysisMap.get(user.getUserId())
                ))
                .toList();

        return AdminStudentListPageResponseDto.of(students, filteredStudents);
    }


    // 관리자가 특정 학생 상세 정보를 조회하는 기능입니다.
    public AdminStudentDetailResponseDto getStudentDetail(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(UserNotFoundException::new);
        TeamUser teamUser = teamUserRepository.findByUserUserId(userId)
                .orElse(null);

        UserAnalysis userAnalysis = userAnalysisRepository.findById(userId)
                .orElse(null);

        return AdminStudentDetailResponseDto.from(user, teamUser, userAnalysis);
    }

    // 이름 검색어가 비어 있으면 전체 허용하고, 값이 있으면 학생 이름에 포함되는지 확인하는 기능입니다.
    private boolean matchesName(User user, String name) {
        return isBlank(name) || user.getName().contains(name.trim());
    }

    // 학번 검색어가 비어 있으면 전체 허용하고, 값이 있으면 userId에 포함되는지 확인하는 기능입니다.
    private boolean matchesUserId(User user, String userId) {
        return isBlank(userId) || user.getUserId().contains(userId.trim());
    }

    // 학년 검색 조건이 없으면 전체 허용하고, 값이 있으면 학생 학년과 일치하는지 확인하는 기능입니다.
    private boolean matchesGrade(User user, Grade grade) {
        return grade == null || user.getGrade() == grade;
    }

    // 설문 완료 여부 검색 조건이 없으면 전체 허용하고, 값이 있으면 학생 설문 완료 여부와 일치하는지 확인하는 기능입니다.
    private boolean matchesSurveyCompleted(User user, Boolean surveyCompleted) {
        return surveyCompleted == null || user.isSurveyCompleted() == surveyCompleted;
    }

    // 희망 직군 검색 조건이 없으면 전체 허용하고, 값이 있으면 학생 역할과 일치하는지 확인하는 기능입니다.
    private boolean matchesStudentRole(User user, TeamUser teamUser, StudentRole studentRole) {
        StudentRole role = teamUser == null ? user.getStudentRole() : teamUser.getStudentRole();
        return studentRole == null || role == studentRole;
    }

    // 문자열이 null이거나 공백인지 확인하는 기능입니다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
