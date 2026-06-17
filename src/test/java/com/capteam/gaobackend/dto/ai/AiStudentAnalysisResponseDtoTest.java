package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiStudentAnalysisResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsReasonAliasToAnalysisResult() throws Exception {
        String json = """
                {
                  "user_id": "stu2301",
                  "name": "장준민",
                  "reason": "React 기반 UI 구현 경험이 있고 협업 성향이 안정적입니다.",
                  "student_level": "MIDDLE"
                }
                """;

        AiStudentAnalysisResponseDto response =
                objectMapper.readValue(json, AiStudentAnalysisResponseDto.class);

        assertThat(response.getUserId()).isEqualTo("stu2301");
        assertThat(response.getAnalysisResult())
                .isEqualTo("React 기반 UI 구현 경험이 있고 협업 성향이 안정적입니다.");
        assertThat(response.getStudentLevel()).isEqualTo("MIDDLE");
    }
}
