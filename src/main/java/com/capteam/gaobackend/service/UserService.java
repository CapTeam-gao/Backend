package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.user.response.HeaderUserResponseDto;
import com.capteam.gaobackend.dto.user.request.UserProfileUpdateRequestDto;
import com.capteam.gaobackend.dto.user.request.UserSurveyRequestDto;
import com.capteam.gaobackend.dto.user.response.StudentSearchResponseDto;
import com.capteam.gaobackend.dto.user.response.UserMeResponseDto;
import com.capteam.gaobackend.dto.user.response.UserSurveyResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Service
public class UserService {

    // 학생 검색, 설문 저장 검증, 본인 조회에 공통으로 사용하는 사용자 저장소입니다.
    // 같은 학년 필터, 학생 계정 검증, userId 기반 선호 팀원 조회를 한 곳에서 처리하기 위해 주입합니다.
    private final UserRepository userRepository;

    // 설문 저장 뒤 AI 학생 분석을 후처리로 연결하기 위한 협력 객체입니다.
    // 검색/선호 팀원 저장과 직접 관련은 없지만 submitMySurvey 흐름에서 함께 사용됩니다.
    private final UserSurveyAnalysisService userSurveyAnalysisService;

    // 설계서가 요구한 "최대 6건" 검색 제한을 서비스 레벨에서 강제하기 위한 상수입니다.
    private static final int STUDENT_SEARCH_LIMIT = 6;

    // 본인 제외 후에도 6건을 채울 수 있게 DB에서는 1건을 더 조회하기 위한 상수입니다.
    private static final int STUDENT_SEARCH_QUERY_LIMIT = STUDENT_SEARCH_LIMIT + 1;


    // 마이페이지 정보를 조회하는 기능입니다.
    @Transactional(readOnly = true)
    public UserMeResponseDto getMyProfile() {
        User user = getAuthenticatedUser();
        return UserMeResponseDto.from(user);
    }

    // 마이페이지 정보를 수정하는 기능입니다.
    @Transactional
    public UserMeResponseDto updateMyProfile(UserProfileUpdateRequestDto dto) {
        User user = getAuthenticatedUser();

        user.updateProfile(
                dto.getStudentRole(),
                dto.getSkill(),
                dto.getExperience(),
                dto.isWantsLeader(),
                resolvePreferredTeammates(user, dto.getPreferredTeammates())
        );

        return UserMeResponseDto.from(user);
    }

    // 같은 학년 학생을 이름 또는 학번(userId 규칙) 일부로 검색하는 기능입니다.
    // 이 프로젝트는 별도 학번 컬럼이 없어서 학생 userId의 숫자부를 학번으로 사용합니다. 예: stu2107 -> 2107
    @Transactional(readOnly = true)
    public List<StudentSearchResponseDto> searchStudents(String keyword) {
        User user = getAuthenticatedUser();
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        if (user.getGrade() == null) {
            return Collections.emptyList();
        }

        // 공백 입력으로 인한 불필요한 like 검색을 막고, 프론트의 부분 검색 계약과 동일하게 trim 후 조회합니다.
        return userRepository.searchStudentsByKeyword(
                        AccountRole.STUDENT,
                        user.getGrade(),
                        keyword.trim(),
                        PageRequest.of(0, STUDENT_SEARCH_QUERY_LIMIT)
                )
                .stream()
                .filter(student -> !student.getUserId().equals(user.getUserId()))
                .limit(STUDENT_SEARCH_LIMIT)
                .map(StudentSearchResponseDto::from)
                .toList();
    }

    // 내 설문 저장 결과를 조회하는 기능입니다.
    @Transactional(readOnly = true)
    public UserSurveyResponseDto getMySurvey() {
        return UserSurveyResponseDto.from(getAuthenticatedUser());
    }

    // 내 설문 응답을 저장하고 성격/개발 성향 점수를 반영하는 기능입니다.
    @Transactional
    public UserSurveyResponseDto submitMySurvey(UserSurveyRequestDto dto) {
        User user = getAuthenticatedUser();

        StudentRole studentRole = resolveStudentRole(dto);
        List<String> skills = resolveSkills(dto);
        List<String> experiences = resolveExperiences(dto);
        List<String> preferredTeammates = resolvePreferredTeammates(user, dto);
        boolean wantsLeader = resolveWantsLeader(dto);
        UserPersonalityScore personalityScores = resolvePersonalityScores(dto);
        UserDevelopmentScore developmentScores = resolveDevelopmentScores(dto);

        if (studentRole == null) {
            throw new IllegalArgumentException("희망 직군을 선택해주세요.");
        }
        if (skills.isEmpty()) {
            throw new IllegalArgumentException("기술 스택을 1개 이상 입력해주세요.");
        }
        if (experiences.isEmpty()) {
            throw new IllegalArgumentException("구현 경험을 1개 이상 입력해주세요.");
        }

        user.completeSurvey(
                studentRole,
                skills,
                experiences,
                wantsLeader,
                preferredTeammates,
                personalityScores,
                developmentScores
        );

        userSurveyAnalysisService.saveSurveyReliability(user, dto);
        tryAnalyzeSubmittedSurvey(user);

        return UserSurveyResponseDto.from(user);
    }

    // AI 서버 장애가 설문 저장 자체를 막지 않도록 분석 실패는 로그로만 남깁니다.
    private void tryAnalyzeSubmittedSurvey(User user) {
        try {
            userSurveyAnalysisService.analyzeSubmittedSurvey(user);
        } catch (RuntimeException e) {
            log.warn("설문은 저장했지만 AI 학생 분석 생성에 실패했습니다. userId={}", user.getUserId(), e);
        }
    }

    // 현재 로그인한 사용자를 조회하는 기능입니다.
    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }


    // 헤더바에 표시할 사용자 정보를 조회하는 기능입니다.
    public HeaderUserResponseDto getHeaderUser(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(UserNotFoundException::new);

        return HeaderUserResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .accountRole(user.getAccountRole())
                .surveyCompleted(user.isSurveyCompleted())
                .build();
    }

    // 설문 요청에서 희망 직군을 결정하는 기능입니다.
    private StudentRole resolveStudentRole(UserSurveyRequestDto dto) {
        if (dto.getStudentRole() != null) {
            StudentRole resolved = mapRole(dto.getStudentRole());
            if (resolved != null) {
                return resolved;
            }
        }

        for (String role : safeList(dto.getSelectedRoles())) {
            StudentRole resolved = mapRole(role);
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    // 프론트 문자열 역할을 StudentRole enum으로 변환하는 기능입니다.
    private StudentRole mapRole(String role) {
        if (role == null) {
            return null;
        }

        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "프론트엔드", "frontend", "front", "react" -> StudentRole.FRONTEND;
            case "백엔드", "backend", "back", "spring" -> StudentRole.BACKEND;
            case "풀스택", "fullstack", "full-stack", "full stack" -> StudentRole.FULLSTACK;
            case "devops", "dev-ops", "dev ops", "인프라" -> StudentRole.DEVOPS;
            case "security", "보안", "시큐리티" -> StudentRole.SECURITY;
            case "ai", "인공지능", "데이터", "ai/data" -> StudentRole.AI;
            case "디자인", "design", "ui/ux", "uiux" -> StudentRole.DESIGN;
            case "게임개발", "game", "game client", "game server" -> StudentRole.GAME;
            case "앱", "app", "android", "ios" -> StudentRole.APP;
            default -> {
                try {
                    yield StudentRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    yield null;
                }
            }
        };
    }

    // 설문 요청에서 기술 스택 목록을 결정하는 기능입니다.
    private List<String> resolveSkills(UserSurveyRequestDto dto) {
        if (dto.getSkill() != null && !dto.getSkill().isEmpty()) {
            return cleanDistinct(dto.getSkill());
        }

        if (dto.getStackText() == null || dto.getStackText().isBlank()) {
            return Collections.emptyList();
        }

        return cleanDistinct(Arrays.asList(dto.getStackText().split(",")));
    }

    // 설문 요청에서 구현 경험 목록을 결정하는 기능입니다.
    private List<String> resolveExperiences(UserSurveyRequestDto dto) {
        if (dto.getExperience() != null && !dto.getExperience().isEmpty()) {
            return cleanDistinct(dto.getExperience());
        }

        if (dto.getExperiences() == null) {
            return Collections.emptyList();
        }

        return cleanDistinct(dto.getExperiences().stream()
                .map(UserSurveyRequestDto.ExperienceItemDto::getValue)
                .toList());
    }

    // 설문 요청에서 선호 팀원 목록을 userId 기준으로 결정하는 기능입니다.
    private List<String> resolvePreferredTeammates(User user, UserSurveyRequestDto dto) {
        List<String> inputs = dto.getPreferredTeammates() != null ? dto.getPreferredTeammates() : dto.getPreferredMembers();
        return resolvePreferredTeammates(user, inputs);
    }

    // 선호 팀원 입력 목록을 userId 기준으로 정규화하고 검증하는 기능입니다.
    private List<String> resolvePreferredTeammates(User user, List<String> inputs) {
        List<String> cleaned = cleanValues(inputs);

        if (cleaned.size() > 3) {
            throw new IllegalArgumentException("선호 팀원은 최대 3명까지 등록할 수 있습니다.");
        }

        Set<String> uniqueUserIds = new LinkedHashSet<>();
        List<String> userIds = new ArrayList<>();
        for (String input : cleaned) {
            String preferredUserId = input.trim();
            if (user.getUserId().equals(preferredUserId)) {
                throw new IllegalArgumentException("본인은 선호 팀원으로 등록할 수 없습니다.");
            }
            if (!uniqueUserIds.add(preferredUserId)) {
                throw new IllegalArgumentException("같은 학생을 선호 팀원으로 중복 등록할 수 없습니다.");
            }
            userIds.add(preferredUserId);
        }

        List<String> resolvedUserIds = new ArrayList<>();
        for (int index = 0; index < cleaned.size(); index++) {
            String preferredUserId = userIds.get(index);
            User preferredUser = userRepository.findById(preferredUserId)
                    .orElseThrow(() -> new IllegalArgumentException("선호 팀원을 찾을 수 없습니다: " + preferredUserId));
            validatePreferredStudent(user, preferredUser);
            resolvedUserIds.add(preferredUser.getUserId());
        }

        return resolvedUserIds;
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

    // 설문 요청에서 팀장 희망 여부를 결정하는 기능입니다.
    private boolean resolveWantsLeader(UserSurveyRequestDto dto) {
        if (dto.getWantsLeader() != null) {
            return dto.getWantsLeader();
        }

        return "O".equalsIgnoreCase(dto.getLeaderPreference());
    }

    // 성격 성향 점수를 항목별 엔티티 값으로 변환하는 기능입니다.
    private UserPersonalityScore resolvePersonalityScores(UserSurveyRequestDto dto) {
        if (dto.getPersonalityScoreAnswers() != null) {
            List<Double> scores = calculateTwoQuestionScores(dto.getPersonalityScoreAnswers(), "성격 성향");
            return new UserPersonalityScore(scores.get(0), scores.get(1), scores.get(2), scores.get(3), scores.get(4));
        }

        UserSurveyRequestDto.PersonalityScoresDto scores = dto.getPersonalityScores();
        if (scores == null) {
            return new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        return new UserPersonalityScore(
                validateCategoryScore(firstNonNull(scores.getIdeaPlanning(), scores.getResponsibility()), "아이디어 기획"),
                validateCategoryScore(scores.getCommunication(), "소통"),
                validateCategoryScore(firstNonNull(scores.getRoleFlexibility(), scores.getCollaboration()), "역할 유연성"),
                validateCategoryScore(firstNonNull(scores.getTimePressure(), scores.getFlexibility()), "시간 압박 대응"),
                validateCategoryScore(firstNonNull(scores.getStaminaFocus(), scores.getEmotionalStability()), "체력 집중")
        );
    }

    // 개발 성향 점수를 항목별 엔티티 값으로 변환하는 기능입니다.
    private UserDevelopmentScore resolveDevelopmentScores(UserSurveyRequestDto dto) {
        if (dto.getDevelopmentScoreAnswers() != null) {
            List<Double> scores = calculateTwoQuestionScores(dto.getDevelopmentScoreAnswers(), "개발 성향");
            return new UserDevelopmentScore(scores.get(0), scores.get(1), scores.get(2), scores.get(3), scores.get(4));
        }

        UserSurveyRequestDto.DevelopmentScoresDto scores =
                dto.getDevelopmentScores() != null ? dto.getDevelopmentScores() : dto.getDevScores();
        if (scores == null) {
            return new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        return new UserDevelopmentScore(
                validateCategoryScore(scores.getImplementation(), "구현 실행력"),
                validateCategoryScore(scores.getProblemSolving(), "문제 해결력"),
                validateCategoryScore(firstNonNull(scores.getCompletionQuality(), scores.getLearningAbility()), "완성도"),
                validateCategoryScore(firstNonNull(scores.getPresentation(), scores.getPlanning()), "발표"),
                validateCategoryScore(scores.getLeadership(), "리더십")
        );
    }

    // 신규 점수 key가 없을 때 과거 key 값을 fallback으로 사용하는 기능입니다.
    private Double firstNonNull(Double primary, Double fallback) {
        return primary != null ? primary : fallback;
    }

    // 10개 문항 원점수를 2문항씩 묶어 5개 항목 평균 점수로 계산하는 기능입니다.
    private List<Double> calculateTwoQuestionScores(List<Integer> answers, String label) {
        if (answers.size() != 10) {
            throw new IllegalArgumentException(label + " 문항 점수는 10개가 필요합니다.");
        }

        List<Double> scores = new ArrayList<>();
        for (int index = 0; index < answers.size(); index += 2) {
            int firstScore = validateAnswerScore(answers.get(index), label);
            int secondScore = validateAnswerScore(answers.get(index + 1), label);
            scores.add((firstScore + secondScore) / 2.0);
        }

        return scores;
    }

    // 문항 원점수가 1~5 사이인지 검증하는 기능입니다.
    private int validateAnswerScore(Integer score, String label) {
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException(label + " 문항 점수는 1~5 사이여야 합니다.");
        }

        return score;
    }

    // 항목별 평균 점수가 0~5 사이인지 검증하는 기능입니다.
    private double validateCategoryScore(Double score, String label) {
        double safeScore = score == null ? 0.0 : score;
        if (safeScore < 0 || safeScore > 5) {
            throw new IllegalArgumentException(label + " 점수는 0~5 사이여야 합니다.");
        }

        return safeScore;
    }

    // 빈 문자열을 제거하고 중복 없는 목록으로 정리하는 기능입니다.
    private List<String> cleanDistinct(List<String> values) {
        Set<String> cleaned = new LinkedHashSet<>();
        for (String value : safeList(values)) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.trim());
            }
        }
        return List.copyOf(cleaned);
    }

    // 빈 문자열을 제거하되 중복 여부는 보존해서 검증할 수 있게 정리하는 기능입니다.
    private List<String> cleanValues(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        for (String value : safeList(values)) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.trim());
            }
        }
        return List.copyOf(cleaned);
    }

    // null 목록을 빈 목록으로 바꾸는 기능입니다.
    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }



}
