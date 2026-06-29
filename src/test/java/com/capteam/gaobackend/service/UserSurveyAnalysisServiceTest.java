package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentAnalysisResponseDto;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.exception.AiServerException;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSurveyAnalysisServiceTest {

    @Mock private AiClient aiClient;
    @Mock private UserAnalysisRepository userAnalysisRepository;
    @Captor private ArgumentCaptor<List<AiStudentPayloadDto>> payloadCaptor;
    @Captor private ArgumentCaptor<UserAnalysis> analysisCaptor;

    private UserSurveyAnalysisService userSurveyAnalysisService;

    @BeforeEach
    void setUp() {
        userSurveyAnalysisService = new UserSurveyAnalysisService(aiClient, userAnalysisRepository);
    }

    @Test
    void savesUserAnalysisFromAiResponseAfterSurvey() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        AiStudentAnalysisResponseDto aiResult = new AiStudentAnalysisResponseDto();
        aiResult.setUserId("stu2301");
        aiResult.setName("홍길동");
        aiResult.setAnalysisResult("문제 해결력이 강한 백엔드 성향입니다.");
        aiResult.setStudentLevel("MIDDLE");

        when(aiClient.runAnalysisForResult(any())).thenReturn(List.of(aiResult));
        when(userAnalysisRepository.findById("stu2301")).thenReturn(Optional.empty());

        userSurveyAnalysisService.analyzeSubmittedSurvey(user);

        verify(aiClient).runAnalysisForResult(payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).hasSize(1);
        assertThat(payloadCaptor.getValue().get(0).getUserId()).isEqualTo("stu2301");

        verify(userAnalysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getUserId()).isEqualTo("stu2301");
        assertThat(analysisCaptor.getValue().getAnalysisResult()).isEqualTo("문제 해결력이 강한 백엔드 성향입니다.");
        assertThat(analysisCaptor.getValue().getStudentLevel()).isEqualTo(StudentLevel.MIDDLE);
    }

    @Test
    void doesNotFailSurveyWhenAiAnalysisFails() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();

        when(aiClient.runAnalysisForResult(any()))
                .thenThrow(new AiServerException("AI 서버 오류"));

        assertThatNoException().isThrownBy(() -> userSurveyAnalysisService.analyzeSubmittedSurvey(user));
    }

    @Test
    void savesUpperMiddleStudentLevelFromAiResponse() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        AiStudentAnalysisResponseDto aiResult = new AiStudentAnalysisResponseDto();
        aiResult.setUserId("stu2301");
        aiResult.setName("홍길동");
        aiResult.setAnalysisResult("중상 수준입니다.");
        aiResult.setStudentLevel("중상");

        when(aiClient.runAnalysisForResult(any())).thenReturn(List.of(aiResult));
        when(userAnalysisRepository.findById("stu2301")).thenReturn(Optional.empty());

        userSurveyAnalysisService.analyzeSubmittedSurvey(user);

        verify(userAnalysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getStudentLevel()).isEqualTo(StudentLevel.UPPER_MIDDLE);
    }
}
