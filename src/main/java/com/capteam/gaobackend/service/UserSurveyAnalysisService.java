package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentAnalysisResponseDto;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
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
}
