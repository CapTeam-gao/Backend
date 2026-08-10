package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentAnalysisResponseDto;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.UserAnalysisStatus;
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
        assertThat(analysisCaptor.getValue().getStatus()).isEqualTo(UserAnalysisStatus.SUCCEEDED);
    }

    // 회귀 테스트: AI 호출이 실패해도 설문 저장을 막던 예외를 다시 던지지 않고(그러면
    // REQUIRES_NEW 트랜잭션이 롤백돼 FAILED 상태 저장까지 함께 사라짐), status만
    // FAILED로 남겨야 관리자 화면이 "분석 중"이 아니라 "분석 실패"로 보여줄 수 있다.
    @Test
    void marksAnalysisFailedInsteadOfThrowingWhenAiAnalysisFails() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();

        when(aiClient.runAnalysisForResult(any()))
                .thenThrow(new AiServerException("AI 서버 오류"));
        when(userAnalysisRepository.findById("stu2301")).thenReturn(Optional.empty());

        userSurveyAnalysisService.analyzeSubmittedSurvey(user);

        verify(userAnalysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getStatus()).isEqualTo(UserAnalysisStatus.FAILED);
    }

    @Test
    void marksAnalysisFailedWhenAiResponseDoesNotContainAnalysisResult() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        AiStudentAnalysisResponseDto aiResult = new AiStudentAnalysisResponseDto();
        aiResult.setUserId("stu2301");
        aiResult.setName("홍길동");
        aiResult.setStudentLevel("MIDDLE");

        when(aiClient.runAnalysisForResult(any())).thenReturn(List.of(aiResult));
        when(userAnalysisRepository.findById("stu2301")).thenReturn(Optional.empty());

        userSurveyAnalysisService.analyzeSubmittedSurvey(user);

        verify(userAnalysisRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getStatus()).isEqualTo(UserAnalysisStatus.FAILED);
    }

    // 회귀 테스트: 이전에 FAILED로 남았던 학생이 재시도(재제출 등)로 성공하면
    // 기존 row가 SUCCEEDED로 갱신되어야 "분석 실패"에 갇히지 않는다.
    @Test
    void updatesExistingFailedAnalysisToSucceededOnRetrySuccess() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        UserAnalysis existingFailed = UserAnalysis.builder()
                .user(user)
                .status(UserAnalysisStatus.FAILED)
                .build();
        AiStudentAnalysisResponseDto aiResult = new AiStudentAnalysisResponseDto();
        aiResult.setUserId("stu2301");
        aiResult.setName("홍길동");
        aiResult.setAnalysisResult("재시도 후 분석 성공");
        aiResult.setStudentLevel("MIDDLE");

        when(aiClient.runAnalysisForResult(any())).thenReturn(List.of(aiResult));
        when(userAnalysisRepository.findById("stu2301")).thenReturn(Optional.of(existingFailed));

        userSurveyAnalysisService.analyzeSubmittedSurvey(user);

        assertThat(existingFailed.getStatus()).isEqualTo(UserAnalysisStatus.SUCCEEDED);
        assertThat(existingFailed.getAnalysisResult()).isEqualTo("재시도 후 분석 성공");
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
