package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiMatchingRequestDto {

    // AI 매칭 대상 학생 목록입니다.
    private List<AiStudentPayloadDto> students;

    // 추천안 재생성 시 AI 서버에 전달할 사용자 프롬프트입니다.
    @JsonProperty("regeneration_prompt")
    private String regenerationPrompt;

    @JsonProperty("teamSize")
    private Integer teamSize;

    public static AiMatchingRequestDto of(List<AiStudentPayloadDto> students, String regenerationPrompt) {
        return AiMatchingRequestDto.builder()
                .students(students)
                .regenerationPrompt(regenerationPrompt)
                .build();
    }

    public static AiMatchingRequestDto hackathon(List<AiStudentPayloadDto> students, int teamSize) {
        return AiMatchingRequestDto.builder()
                .students(students)
                .teamSize(teamSize)
                .build();
    }
}
