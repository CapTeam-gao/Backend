package com.capteam.gaobackend.ai;

import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.exception.AiServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class AiClient {

    private final RestClient restClient;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    public AiClient(@Value("${ai.server.base-url}") String aiServerBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);       // 연결 타임아웃 10초
        factory.setReadTimeout(600_000);          // 읽기 타임아웃 10분 (AI 매칭 시간 고려)

        this.restClient = RestClient.builder()
                .baseUrl(aiServerBaseUrl)
                .requestFactory(factory)
                .build();
        this.aiServerBaseUrl = aiServerBaseUrl;
    }

    // 백엔드 학생 데이터를 JSON으로 AI 서버에 전달해 분석을 실행하는 기능입니다.
    public void runAnalysis(List<AiStudentPayloadDto> students) {
        try {
            var request = restClient.post()
                    .uri("/analysis/run")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if (students != null) request.body(students);
            request.retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new AiServerException("AI 학생 분석 실행에 실패했습니다. AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException("AI 학생 분석 실행에 실패했습니다. AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }

    // 백엔드 학생 데이터를 JSON으로 AI 서버에 전달해 팀 매칭을 실행하는 기능입니다.
    public AiTeamSummaryResponseDto runMatching(List<AiStudentPayloadDto> students) {
        try {
            var request = restClient.post()
                    .uri("/matching/run")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if (students != null) request.body(students);
            AiTeamSummaryResponseDto response = request.retrieve().body(AiTeamSummaryResponseDto.class);
            if (response == null) throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 응답이 비어 있습니다.");
            return response;
        } catch (RestClientResponseException e) {
            throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }

    // AI 서버에서 현재 생성된 팀 요약 결과를 조회하는 기능입니다.
    public AiTeamSummaryResponseDto getTeamSummary() {
        try {
            AiTeamSummaryResponseDto response = restClient.get()
                    .uri("/teams/summary")
                    .retrieve()
                    .body(AiTeamSummaryResponseDto.class);
            if (response == null) throw new AiServerException("AI 팀 요약 결과를 조회하지 못했습니다. AI 서버 응답이 비어 있습니다.");
            return response;
        } catch (RestClientResponseException e) {
            throw new AiServerException("AI 팀 요약 결과를 조회하지 못했습니다. AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException("AI 팀 요약 결과를 조회하지 못했습니다. AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }
}
