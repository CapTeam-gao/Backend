package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiMatchingRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesRegenerationPromptWithSnakeCaseField() throws Exception {
        AiMatchingRequestDto request = AiMatchingRequestDto.of(
                List.of(),
                "백엔드 역할을 강화해줘"
        );

        JsonNode json = objectMapper.valueToTree(request);

        assertThat(json.has("students")).isTrue();
        assertThat(json.get("regeneration_prompt").asText()).isEqualTo("백엔드 역할을 강화해줘");
        assertThat(json.has("regenerationPrompt")).isFalse();
    }
}
