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


    // 마이페이지 조회
    @Transactional(readOnly = true)
    public UserMeResponseDto getMyProfile() {
        User user = getAuthenticatedUser();
        return UserMeResponseDto.from(user);
    }

    // 마이페이지 수정
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

    @Transactional(readOnly = true)
    public UserSurveyResponseDto getMySurvey() {
        return UserSurveyResponseDto.from(getAuthenticatedUser());
    }

    @Transactional
    public UserSurveyResponseDto submitMySurvey(UserSurveyRequestDto dto) {
        User user = getAuthenticatedUser();

        StudentRole studentRole = resolveStudentRole(dto);
        List<String> skills = resolveSkills(dto);
        List<String> experiences = resolveExperiences(dto);
        List<String> preferredTeammates = resolvePreferredTeammates(user, dto);
        boolean wantsLeader = resolveWantsLeader(dto);
        List<Integer> personalityScores = validateScores(dto.getPersonalityScores(), "성격 성향");
        List<Integer> developmentScores = validateScores(
                dto.getDevelopmentScores() != null ? dto.getDevelopmentScores() : dto.getDevScores(),
                "개발 성향"
        );

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

    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }


    //헤더바에 학번, 이름을 보내주는 코드
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

    private List<String> resolveSkills(UserSurveyRequestDto dto) {
        if (dto.getSkill() != null && !dto.getSkill().isEmpty()) {
            return cleanDistinct(dto.getSkill());
        }

        if (dto.getStackText() == null || dto.getStackText().isBlank()) {
            return Collections.emptyList();
        }

        return cleanDistinct(Arrays.asList(dto.getStackText().split(",")));
    }

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

    private String resolvePreferredUserId(String input) {
        Matcher matcher = PREFERRED_MEMBER_PATTERN.matcher(input.trim());
        if (matcher.matches()) {
            return "stu" + matcher.group(1);
        }

        return input.trim();
    }

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

    private boolean resolveWantsLeader(UserSurveyRequestDto dto) {
        if (dto.getWantsLeader() != null) {
            return dto.getWantsLeader();
        }

        return "O".equalsIgnoreCase(dto.getLeaderPreference());
    }

    private List<Integer> validateScores(List<Integer> scores, String label) {
        if (scores == null) {
            return Collections.emptyList();
        }

        for (Integer score : scores) {
            if (score == null || score < 1 || score > 5) {
                throw new IllegalArgumentException(label + " 점수는 1~5 사이여야 합니다.");
            }
        }

        return List.copyOf(scores);
    }

    private List<String> cleanDistinct(List<String> values) {
        Set<String> cleaned = new LinkedHashSet<>();
        for (String value : safeList(values)) {
            if (value != null && !value.isBlank()) {
                cleaned.add(value.trim());
            }
        }
        return List.copyOf(cleaned);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : values;
    }



}
