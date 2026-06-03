package com.capteam.gaobackend.ai;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.exception.AiServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    public AiTeamSummaryResponseDto getTeamSummary() {
        return requestTeamSummary("/teams/summary", "AI 팀 요약 결과를 조회하지 못했습니다.");
    }

    public AiTeamSummaryResponseDto runMatching() {
        return requestTeamSummary("/matching/run", "AI 팀 매칭 실행에 실패했습니다.");
    }

    private AiTeamSummaryResponseDto requestTeamSummary(String uri, String errorMessage) {
        try {
            AiTeamSummaryResponseDto response = restClientBuilder
                    .baseUrl(aiServerBaseUrl)
                    .build()
                    .method(uri.equals("/matching/run") ? org.springframework.http.HttpMethod.POST : org.springframework.http.HttpMethod.GET)
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
