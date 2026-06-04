package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.user.response.HeaderUserResponseDto;
import com.capteam.gaobackend.dto.user.request.UserProfileUpdateRequestDto;
import com.capteam.gaobackend.dto.user.request.UserSurveyRequestDto;
import com.capteam.gaobackend.dto.user.response.StudentDetailResponseDto;
import com.capteam.gaobackend.dto.user.response.StudentListResponseDto;
import com.capteam.gaobackend.dto.user.response.UserMeResponseDto;
import com.capteam.gaobackend.dto.user.response.UserSurveyResponseDto;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import com.capteam.gaobackend.repository.UserRepository;
//import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private static final Pattern PREFERRED_MEMBER_PATTERN = Pattern.compile("^(?:stu)?(\\d{4})\\s+(.+)$", Pattern.CASE_INSENSITIVE);


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
                dto.getPreferredTeammates()
        );

        return UserMeResponseDto.from(user);
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

        return UserSurveyResponseDto.from(user);
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
            return dto.getStudentRole();
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
            case "ai", "인공지능", "데이터", "ai/data" -> StudentRole.AI;
            case "디자인", "design", "ui/ux", "uiux" -> StudentRole.DESIGN;
            case "앱", "app", "android", "ios", "게임개발", "game" -> StudentRole.APP;
            case "풀스택", "fullstack", "full-stack", "devops" -> StudentRole.BACKEND;
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
        List<String> cleaned = cleanDistinct(inputs);

        if (cleaned.size() > 3) {
            throw new IllegalArgumentException("선호 팀원은 최대 3명까지 등록할 수 있습니다.");
        }

        List<String> userIds = new ArrayList<>();
        for (String input : cleaned) {
            String preferredUserId = resolvePreferredUserId(input);
            if (user.getUserId().equals(preferredUserId)) {
                throw new IllegalArgumentException("본인은 선호 팀원으로 등록할 수 없습니다.");
            }

            User preferredUser = userRepository.findById(preferredUserId)
                    .orElseThrow(() -> new IllegalArgumentException("선호 팀원을 찾을 수 없습니다: " + input));
            validatePreferredName(input, preferredUser);
            userIds.add(preferredUser.getUserId());
        }

        return userIds;
    }

    // 선호 팀원 입력값에서 userId를 추출하는 기능입니다.
    private String resolvePreferredUserId(String input) {
        Matcher matcher = PREFERRED_MEMBER_PATTERN.matcher(input.trim());
        if (matcher.matches()) {
            return "stu" + matcher.group(1);
        }

        return input.trim();
    }

    // 선호 팀원 입력값의 학번과 이름이 실제 사용자와 일치하는지 검증하는 기능입니다.
    private void validatePreferredName(String input, User preferredUser) {
        Matcher matcher = PREFERRED_MEMBER_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            return;
        }

        String name = matcher.group(2).trim();
        if (!preferredUser.getName().equals(name)) {
            throw new IllegalArgumentException("선호 팀원 학번과 이름이 일치하지 않습니다: " + input);
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
            List<Integer> scores = calculateTwoQuestionScores(dto.getPersonalityScoreAnswers(), "성격 성향");
            return new UserPersonalityScore(scores.get(0), scores.get(1), scores.get(2), scores.get(3), scores.get(4));
        }

        UserSurveyRequestDto.PersonalityScoresDto scores = dto.getPersonalityScores();
        if (scores == null) {
            return new UserPersonalityScore(0, 0, 0, 0, 0);
        }

        return new UserPersonalityScore(
                validateCategoryScore(scores.getCommunication(), "소통"),
                validateCategoryScore(scores.getResponsibility(), "책임감"),
                validateCategoryScore(scores.getCollaboration(), "협업"),
                validateCategoryScore(scores.getFlexibility(), "유연성"),
                validateCategoryScore(scores.getEmotionalStability(), "감정 안정성")
        );
    }

    // 개발 성향 점수를 항목별 엔티티 값으로 변환하는 기능입니다.
    private UserDevelopmentScore resolveDevelopmentScores(UserSurveyRequestDto dto) {
        if (dto.getDevelopmentScoreAnswers() != null) {
            List<Integer> scores = calculateTwoQuestionScores(dto.getDevelopmentScoreAnswers(), "개발 성향");
            return new UserDevelopmentScore(scores.get(0), scores.get(1), scores.get(2), scores.get(3), scores.get(4));
        }

        UserSurveyRequestDto.DevelopmentScoresDto scores =
                dto.getDevelopmentScores() != null ? dto.getDevelopmentScores() : dto.getDevScores();
        if (scores == null) {
            return new UserDevelopmentScore(0, 0, 0, 0, 0);
        }

        return new UserDevelopmentScore(
                validateCategoryScore(scores.getLeadership(), "리더십"),
                validateCategoryScore(scores.getProblemSolving(), "문제 해결력"),
                validateCategoryScore(scores.getImplementation(), "구현 실행력"),
                validateCategoryScore(scores.getLearningAbility(), "학습 성장성"),
                validateCategoryScore(scores.getPlanning(), "기획 정리력")
        );
    }

    // 10개 문항 원점수를 2문항씩 묶어 5개 항목 평균 점수로 계산하는 기능입니다.
    private List<Integer> calculateTwoQuestionScores(List<Integer> answers, String label) {
        if (answers.size() != 10) {
            throw new IllegalArgumentException(label + " 문항 점수는 10개가 필요합니다.");
        }

        List<Integer> scores = new ArrayList<>();
        for (int index = 0; index < answers.size(); index += 2) {
            int firstScore = validateAnswerScore(answers.get(index), label);
            int secondScore = validateAnswerScore(answers.get(index + 1), label);
            scores.add(Math.round((firstScore + secondScore) / 2.0f));
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
    private int validateCategoryScore(Integer score, String label) {
        int safeScore = score == null ? 0 : score;
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

    // null 목록을 빈 목록으로 바꾸는 기능입니다.
    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }



}
