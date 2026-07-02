package com.capteam.gaobackend.ai;

import com.capteam.gaobackend.dto.ai.AiMatchingRequestDto;
import com.capteam.gaobackend.dto.ai.AiStudentAnalysisResponseDto;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.exception.AiServerException;
import com.capteam.gaobackend.exception.MatchingJobCancelledException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiClient {

    private final RestClient restClient;
    private final HttpClient cancellableHttpClient;
    private final ObjectMapper objectMapper;
    // 실행 중인 HTTP 요청을 jobId로 찾아 Future.cancel() 할 수 있도록 보관합니다.
    private final Map<String, CompletableFuture<HttpResponse<String>>> matchingRequests = new ConcurrentHashMap<>();
    // HTTP 요청 등록 직전에 취소가 들어오는 경쟁 조건을 처리하기 위한 임시 취소 목록입니다.
    private final Set<String> cancelledMatchingJobs = ConcurrentHashMap.newKeySet();

    private final String aiServerBaseUrl;

    public AiClient(@Value("${ai.server.base-url}") String aiServerBaseUrl, ObjectMapper objectMapper) {
        String normalizedAiServerBaseUrl = normalizeBaseUrl(aiServerBaseUrl);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);       // 연결 타임아웃 10초
        factory.setReadTimeout(600_000);          // 읽기 타임아웃 10분 (AI 매칭 시간 고려)

        this.restClient = RestClient.builder()
                .baseUrl(normalizedAiServerBaseUrl)
                .requestFactory(factory)
                .build();
        this.cancellableHttpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
        this.aiServerBaseUrl = normalizedAiServerBaseUrl;
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

    public List<AiStudentAnalysisResponseDto> runAnalysisForResult(List<AiStudentPayloadDto> students) {
        try {
            var request = restClient.post()
                    .uri("/analysis/run")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            request.body(students == null ? List.of() : students);

            String responseBody = request.retrieve().body(String.class);
            return readAnalysisResults(responseBody);
        } catch (RestClientResponseException e) {
            throw new AiServerException("AI 학생 분석 실행에 실패했습니다. AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException("AI 학생 분석 실행에 실패했습니다. AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }

    // 백엔드 학생 데이터를 JSON으로 AI 서버에 전달해 팀 매칭을 실행하는 기능입니다.
    public AiTeamSummaryResponseDto runMatching(List<AiStudentPayloadDto> students) {
        return runMatchingWithPrompt(students, null);
    }

    public AiTeamSummaryResponseDto runMatchingWithPrompt(List<AiStudentPayloadDto> students, String regenerationPrompt) {
        try {
            var request = restClient.post()
                    .uri("/matching/run")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            request.body(buildMatchingRequestBody(students, regenerationPrompt));
            AiTeamSummaryResponseDto response = request.retrieve().body(AiTeamSummaryResponseDto.class);
            if (response == null) throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 응답이 비어 있습니다.");
            return response;
        } catch (RestClientResponseException e) {
            throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 상태 코드: " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        }
    }

    public AiTeamSummaryResponseDto runMatching(List<AiStudentPayloadDto> students, String jobId) {
        return runMatching(students, jobId, null);
    }

    public AiTeamSummaryResponseDto runMatching(List<AiStudentPayloadDto> students, String jobId, String regenerationPrompt) {
        // 실행기가 AI 요청을 보내기 전에 이미 취소된 작업이면 외부 호출을 시작하지 않습니다.
        if (cancelledMatchingJobs.remove(jobId)) {
            throw new MatchingJobCancelledException(jobId);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(aiServerUri("/matching/run"))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                // AI 서버에서도 같은 작업 ID로 LLM 실행 상태를 관리하도록 전달합니다.
                .header("X-Matching-Job-Id", jobId)
                .POST(HttpRequest.BodyPublishers.ofString(writeJson(students, regenerationPrompt)))
                .build();

        CompletableFuture<HttpResponse<String>> responseFuture = cancellableHttpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        matchingRequests.put(jobId, responseFuture);
        // 요청 생성과 Map 등록 사이에 들어온 취소도 놓치지 않고 즉시 반영합니다.
        if (cancelledMatchingJobs.remove(jobId)) {
            responseFuture.cancel(true);
        }

        try {
            HttpResponse<String> response = responseFuture.join();
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 상태 코드: " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), AiTeamSummaryResponseDto.class);
        } catch (CancellationException e) {
            throw new MatchingJobCancelledException(jobId);
        } catch (CompletionException e) {
            if (responseFuture.isCancelled()) {
                throw new MatchingJobCancelledException(jobId);
            }
            throw new AiServerException("AI 팀 매칭 실행에 실패했습니다. AI 서버 연결 주소를 확인해주세요: " + aiServerBaseUrl, e);
        } catch (JsonProcessingException e) {
            throw new AiServerException("AI 팀 매칭 응답을 해석하지 못했습니다.", e);
        } finally {
            matchingRequests.remove(jobId, responseFuture);
            cancelledMatchingJobs.remove(jobId);
        }
    }

    public void cancelMatching(String jobId) {
        // 로컬 HTTP 연결 취소와 AI 서버 작업 취소를 모두 수행합니다.
        cancelledMatchingJobs.add(jobId);
        CompletableFuture<HttpResponse<String>> responseFuture = matchingRequests.remove(jobId);
        if (responseFuture != null) {
            responseFuture.cancel(true);
        }

        HttpRequest cancelRequest = HttpRequest.newBuilder()
                // AI 서버가 이 API를 구현해야 이미 시작된 LLM 호출까지 확실히 중단됩니다.
                .uri(aiServerUri("/matching/jobs/" + jobId))
                .timeout(Duration.ofSeconds(5))
                .DELETE()
                .build();
        cancellableHttpClient.sendAsync(cancelRequest, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ignored -> null);
    }

    private String writeJson(List<AiStudentPayloadDto> students, String regenerationPrompt) {
        try {
            return objectMapper.writeValueAsString(buildMatchingRequestBody(students, regenerationPrompt));
        } catch (JsonProcessingException e) {
            throw new AiServerException("AI 팀 매칭 요청 데이터를 생성하지 못했습니다.", e);
        }
    }

    private Object buildMatchingRequestBody(List<AiStudentPayloadDto> students, String regenerationPrompt) {
        List<AiStudentPayloadDto> safeStudents = students == null ? List.of() : students;
        if (regenerationPrompt == null || regenerationPrompt.isBlank()) {
            return safeStudents;
        }
        return AiMatchingRequestDto.of(safeStudents, regenerationPrompt);
    }

    private URI aiServerUri(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return URI.create(aiServerBaseUrl + normalizedPath);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("ai.server.base-url is required");
        }
        return baseUrl.replaceAll("/+$", "");
    }

    private List<AiStudentAnalysisResponseDto> readAnalysisResults(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultNode = findAnalysisResultNode(root);
            if (resultNode == null || resultNode.isNull()) {
                return List.of();
            }
            if (resultNode.isArray()) {
                List<AiStudentAnalysisResponseDto> results = new ArrayList<>();
                for (JsonNode item : resultNode) {
                    results.add(objectMapper.treeToValue(item, AiStudentAnalysisResponseDto.class));
                }
                return results;
            }
            return List.of(objectMapper.treeToValue(resultNode, AiStudentAnalysisResponseDto.class));
        } catch (JsonProcessingException e) {
            throw new AiServerException("AI 학생 분석 응답을 해석하지 못했습니다.", e);
        }
    }

    private JsonNode findAnalysisResultNode(JsonNode root) {
        if (root == null || root.isNull() || root.isArray()) {
            return root;
        }

        for (String fieldName : List.of("data", "results", "analyses", "analysis_results", "students")) {
            JsonNode child = root.get(fieldName);
            if (child != null && !child.isNull()) {
                return child;
            }
        }
        return root;
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
