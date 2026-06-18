package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentAnalysisResponseDto;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.user.request.UserSurveyRequestDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.ResponseReliability;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.exception.AiServerException;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSurveyAnalysisService {

    private final AiClient aiClient;
    private final UserAnalysisRepository userAnalysisRepository;

    public void saveSurveyReliability(User user, UserSurveyRequestDto dto) {
        ResponseReliability responseReliability = dto.getResponseReliability();
        Integer inconsistentAnswers = validateNonNegative(dto.getInconsistentAnswers(), "전체 불일치 응답 수");
        Integer personalityInconsistentCount =
                validateNonNegative(dto.getPersonalityInconsistentCount(), "성격 성향 불일치 응답 수");
        Integer developmentInconsistentCount =
                validateNonNegative(dto.getDevelopmentInconsistentCount(), "개발 성향 불일치 응답 수");

        userAnalysisRepository.findById(user.getUserId()).ifPresentOrElse(
                analysis -> analysis.updateSurveyReliability(
                        responseReliability,
                        inconsistentAnswers,
                        personalityInconsistentCount,
                        developmentInconsistentCount
                ),
                () -> userAnalysisRepository.save(UserAnalysis.builder()
                        .user(user)
                        .responseReliability(responseReliability)
                        .inconsistentAnswers(inconsistentAnswers)
                        .personalityInconsistentCount(personalityInconsistentCount)
                        .developmentInconsistentCount(developmentInconsistentCount)
                        .build())
        );
    }

    public void analyzeSubmittedSurvey(User user) {
        try {
            List<AiStudentAnalysisResponseDto> results =
                    aiClient.runAnalysisForResult(List.of(AiStudentPayloadDto.from(user)));
            AiStudentAnalysisResponseDto result = findUserAnalysisResult(user, results);
            if (result == null || result.getAnalysisResult() == null || result.getAnalysisResult().isBlank()) {
                log.warn("AI 학생 분석 응답에서 사용자 분석 결과를 찾지 못했습니다. userId={}", user.getUserId());
                return;
            }

            StudentLevel studentLevel = parseStudentLevel(result.getStudentLevel());
            userAnalysisRepository.findById(user.getUserId()).ifPresentOrElse(
                    analysis -> analysis.updateAnalysisResult(result.getAnalysisResult(), studentLevel),
                    () -> userAnalysisRepository.save(UserAnalysis.builder()
                            .user(user)
                            .analysisResult(result.getAnalysisResult())
                            .studentLevel(studentLevel)
                            .build())
            );
        } catch (AiServerException e) {
            log.warn("설문 저장 후 AI 학생 분석 생성에 실패했습니다. userId={}", user.getUserId(), e);
        }
    }

    private AiStudentAnalysisResponseDto findUserAnalysisResult(
            User user,
            List<AiStudentAnalysisResponseDto> results
    ) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        return results.stream()
                .filter(result -> user.getUserId().equals(result.getUserId()))
                .findFirst()
                .orElseGet(() -> results.stream()
                        .filter(result -> user.getName().equals(result.getName()))
                        .findFirst()
                        .orElse(results.get(0)));
    }

    private StudentLevel parseStudentLevel(String level) {
        if (level == null || level.isBlank()) {
            return null;
        }

        String normalized = level.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "UPPER", "HIGH", "상" -> StudentLevel.UPPER;
            case "MIDDLE", "MID", "MEDIUM", "중" -> StudentLevel.MIDDLE;
            case "LOWER", "LOW", "하" -> StudentLevel.LOWER;
            default -> null;
        };
    }

    private Integer validateNonNegative(Integer value, String label) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(label + "는 0 이상이어야 합니다.");
        }

        return value;
    }
}
