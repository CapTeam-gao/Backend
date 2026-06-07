package com.capteam.gaobackend.ai;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.exception.AiServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class AiClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    // AI 서버에서 저장된 학생 원본 데이터를 분석 결과 테이블로 반영하는 기능입니다.
    public void runAnalysis() {
        requestWithoutBody("/analysis/run", "AI 학생 분석 실행에 실패했습니다.");
    }

    // AI 서버에서 현재 생성된 팀 요약 결과를 조회하는 기능입니다.
    public AiTeamSummaryResponseDto getTeamSummary() {
        return requestTeamSummary(HttpMethod.GET, "/teams/summary", "AI 팀 요약 결과를 조회하지 못했습니다.");
    }

    // AI 서버에서 최신 분석 결과 기반으로 팀 매칭을 실행하는 기능입니다.
    public AiTeamSummaryResponseDto runMatching() {
        return requestTeamSummary(HttpMethod.POST, "/matching/run", "AI 팀 매칭 실행에 실패했습니다.");
    }

    // 응답 본문이 없는 AI 서버 실행 API를 호출하고 실패 시 공통 예외로 변환하는 기능입니다.
    private void requestWithoutBody(String uri, String errorMessage) {
        try {
            restClientBuilder
                    .baseUrl(aiServerBaseUrl)
                    .build()
                    .post()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new AiServerException(errorMessage + " AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException(errorMessage + " AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }

    // AI 서버 팀 요약/매칭 응답을 DTO로 받아오고 실패 시 공통 예외로 변환하는 기능입니다.
    private AiTeamSummaryResponseDto requestTeamSummary(HttpMethod method, String uri, String errorMessage) {
        try {
            AiTeamSummaryResponseDto response = restClientBuilder
                    .baseUrl(aiServerBaseUrl)
                    .build()
                    .method(method)
                    .uri(uri)
                    .retrieve()
                    .body(AiTeamSummaryResponseDto.class);

            if (response == null) {
                throw new AiServerException(errorMessage + " AI 서버 응답이 비어 있습니다.");
            }

            return response;
        } catch (RestClientResponseException e) {
            throw new AiServerException(errorMessage + " AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException(errorMessage + " AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }
}
