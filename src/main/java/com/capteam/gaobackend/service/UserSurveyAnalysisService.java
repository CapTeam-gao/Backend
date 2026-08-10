package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentAnalysisResponseDto;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.user.request.UserSurveyRequestDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.ResponseReliability;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.UserAnalysisStatus;
import com.capteam.gaobackend.exception.AiServerException;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSurveyAnalysisService {

    private final AiClient aiClient;
    private final UserAnalysisRepository userAnalysisRepository;

    @Transactional
    public void saveSurveyReliability(User user, UserSurveyRequestDto dto) {
        ResponseReliability responseReliability = parseResponseReliability(dto.getResponseReliability());
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

    // 여기서 실패해도 예외를 다시 던지지 않고 status를 FAILED로 남기고 끝냅니다. 예외를
    // 다시 던지면 이 메서드의 트랜잭션(REQUIRES_NEW)이 롤백되면서 방금 저장한 FAILED
    // 상태까지 함께 사라져, 관리자 화면에 "분석 중"만 계속 보이던 원래 버그가 재발합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void analyzeSubmittedSurvey(User user) {
        try {
            List<AiStudentAnalysisResponseDto> results =
                    aiClient.runAnalysisForResult(List.of(AiStudentPayloadDto.from(user)));
            AiStudentAnalysisResponseDto result = findUserAnalysisResult(user, results);
            if (result == null || result.getAnalysisResult() == null || result.getAnalysisResult().isBlank()) {
                throw new IllegalStateException("AI 학생 분석 응답에서 사용자 분석 결과를 찾지 못했습니다. userId=" + user.getUserId());
            }

            StudentLevel studentLevel = parseStudentLevel(result.getStudentLevel());
            userAnalysisRepository.findById(user.getUserId()).ifPresentOrElse(
                    analysis -> analysis.updateAnalysisResult(result.getAnalysisResult(), studentLevel),
                    () -> userAnalysisRepository.save(UserAnalysis.builder()
                            .user(user)
                            .analysisResult(result.getAnalysisResult())
                            .studentLevel(studentLevel)
                            .status(UserAnalysisStatus.SUCCEEDED)
                            .build())
            );
        } catch (AiServerException | IllegalStateException e) {
            log.error("설문 저장 후 AI 학생 분석 생성에 실패했습니다. userId={}", user.getUserId(), e);
            markAnalysisFailed(user);
        }
    }

    private void markAnalysisFailed(User user) {
        userAnalysisRepository.findById(user.getUserId()).ifPresentOrElse(
                UserAnalysis::markAnalysisFailed,
                () -> userAnalysisRepository.save(UserAnalysis.builder()
                        .user(user)
                        .status(UserAnalysisStatus.FAILED)
                        .build())
        );
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
            // AI 서버가 한글 또는 여러 영문 alias로 중상/중하를 내려줘도 같은 enum으로 저장합니다.
            case "UPPER_MIDDLE", "UPPER-MIDDLE", "HIGH_MIDDLE", "HIGH-MIDDLE", "중상" -> StudentLevel.UPPER_MIDDLE;
            case "MIDDLE", "MID", "MEDIUM", "중" -> StudentLevel.MIDDLE;
            case "LOWER_MIDDLE", "LOWER-MIDDLE", "LOW_MIDDLE", "LOW-MIDDLE", "중하" -> StudentLevel.LOWER_MIDDLE;
            case "LOWER", "LOW", "하" -> StudentLevel.LOWER;
            default -> null;
        };
    }

    private ResponseReliability parseResponseReliability(String reliability) {
        if (reliability == null || reliability.isBlank()) {
            return null;
        }

        String normalized = reliability.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "HIGH", "높음", "상" -> ResponseReliability.HIGH;
            case "MEDIUM", "MID", "보통", "중" -> ResponseReliability.MEDIUM;
            case "LOW", "낮음", "하" -> ResponseReliability.LOW;
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
//
//
