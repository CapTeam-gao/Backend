package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminStudentDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListPageResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentSearchResponseDto;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStudentService {

    private static final int MANUAL_TEAM_STUDENT_SEARCH_LIMIT = 10;

    // 관리자 학생 목록/상세 조회에 필요한 팀원 정보를 조회하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 학생별 AI 분석 결과를 함께 보여주기 위한 Repository 필드입니다.
    private final UserAnalysisRepository userAnalysisRepository;

    // 팀 배정 전 학생까지 목록에 포함하기 위해 사용자 정보를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 프로젝트 기획서에 작성된 팀 이름을 조회하는 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

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
        Map<Long, String> projectTeamNameMap = teamProjectRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        teamProject -> teamProject.getTeam().getId(),
                        TeamProject::getTeamName,
                        (first, second) -> first
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
                        userAnalysisMap.get(user.getUserId()),
                        resolveProjectTeamName(teamUserMap.get(user.getUserId()), projectTeamNameMap)
                ))
                .toList();

        return AdminStudentListPageResponseDto.of(students, filteredStudents);
    }

    // 직접 팀 구성 화면에서 이름, 학번 또는 직군 한 단어로 학생을 검색하는 기능입니다.
    public List<AdminStudentSearchResponseDto> searchStudentsForManualTeam(Grade grade, String keyword) {
        if (grade == null) {
            throw new IllegalArgumentException("학년을 선택해주세요.");
        }
        if (isBlank(keyword)) {
            return List.of();
        }

        String trimmedKeyword = keyword.trim();
        StudentRole studentRole = resolveStudentRoleKeyword(trimmedKeyword);
        return userRepository.searchStudentsForManualTeam(
                        AccountRole.STUDENT,
                        grade,
                        trimmedKeyword,
                        studentRole,
                        PageRequest.of(0, MANUAL_TEAM_STUDENT_SEARCH_LIMIT)
                )
                .stream()
                .map(AdminStudentSearchResponseDto::from)
                .toList();
    }


    // 관리자가 특정 학생 상세 정보를 조회하는 기능입니다.
    public AdminStudentDetailResponseDto getStudentDetail(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(UserNotFoundException::new);
        List<TeamUser> teamUsers = teamUserRepository.findAllByUserUserId(userId);
        if (teamUsers.size() > 1) {
            throw new IllegalStateException("학생이 여러 팀에 중복 배정되어 있습니다: " + userId);
        }
        TeamUser teamUser = teamUsers.stream()
                .findFirst()
                .orElse(null);

        UserAnalysis userAnalysis = userAnalysisRepository.findById(userId)
                .orElse(null);

        UserDevelopmentScore userDevelopmentScore = user.getDevelopmentScores();


        UserPersonalityScore userPersonalityScore = user.getPersonalityScores();


        String projectTeamName = teamUser == null ? null : teamProjectRepository.findByTeamId(teamUser.getTeam().getId())
                .map(TeamProject::getTeamName)
                .orElse(null);

        return AdminStudentDetailResponseDto.from(
                user,
                teamUser,
                userAnalysis,
                userDevelopmentScore,
                userPersonalityScore,
                projectTeamName
        );
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

    // 직접 팀 구성 검색어를 학생 희망 직군 enum으로 변환하는 기능입니다.
    private StudentRole resolveStudentRoleKeyword(String keyword) {
        String normalized = keyword.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]", "");
        return switch (normalized) {
            case "프론트엔드", "프론트", "frontend", "front", "react" -> StudentRole.FRONTEND;
            case "백엔드", "백", "backend", "back", "spring" -> StudentRole.BACKEND;
            case "ai", "인공지능", "데이터" -> StudentRole.AI;
            case "앱", "app", "android", "ios" -> StudentRole.APP;
            case "디자인", "design", "uiux" -> StudentRole.DESIGN;
            case "devops", "인프라" -> StudentRole.DEVOPS;
            case "게임", "게임개발", "game" -> StudentRole.GAME;
            case "풀스택", "fullstack" -> StudentRole.FULLSTACK;
            case "보안", "security", "시큐리티" -> StudentRole.SECURITY;
            default -> {
                try {
                    yield StudentRole.valueOf(keyword.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    // 팀원 정보가 있으면 프로젝트 기획서의 팀명을 찾아 반환하는 기능입니다.
    private String resolveProjectTeamName(TeamUser teamUser, Map<Long, String> projectTeamNameMap) {
        return teamUser == null ? null : projectTeamNameMap.get(teamUser.getTeam().getId());
    }

    // 문자열이 null이거나 공백인지 확인하는 기능입니다.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
