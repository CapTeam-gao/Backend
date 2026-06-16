package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiStudentAnalysisResponseDto {

    @JsonProperty("user_id")
    @JsonAlias({"userId", "student_id", "studentId"})
    private String userId;

    private String name;

    @JsonProperty("analysis_result")
    @JsonAlias({"analysisResult", "analysis", "result", "strength"})
    private String analysisResult;

    @JsonProperty("student_level")
    @JsonAlias({"studentLevel", "skill_level", "skillLevel", "level"})
    private String studentLevel;
}
