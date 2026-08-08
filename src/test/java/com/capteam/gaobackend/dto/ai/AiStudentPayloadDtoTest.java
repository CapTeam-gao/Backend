package com.capteam.gaobackend.dto.ai;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiStudentPayloadDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void copiesUserCollectionsToPlainLists() {
        User user = User.builder()
                .userId("stu2401")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .grade(Grade.GRADE_2)
                .build();
        List<String> skills = new ArrayList<>(List.of("Spring"));
        List<String> experiences = new ArrayList<>(List.of("게시판 구현"));
        List<String> preferredTeammates = new ArrayList<>(List.of("stu2402"));
        user.completeSurvey(
                StudentRole.BACKEND,
                skills,
                experiences,
                true,
                preferredTeammates,
                null,
                null
        );

        AiStudentPayloadDto payload = AiStudentPayloadDto.from(user);
        skills.add("React");
        experiences.add("채팅 구현");
        preferredTeammates.add("stu2403");

        assertThat(payload.getStack()).containsExactly("Spring");
        assertThat(payload.getExperience()).containsExactly("게시판 구현");
        assertThat(payload.getPreferredMembers()).containsExactly("stu2402");
    }

    @Test
    void serializesNewAndLegacyTraitFieldsForAiCompatibility() throws JsonProcessingException {
        User user = User.builder()
                .userId("stu2401")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .grade(Grade.GRADE_2)
                .build();
        user.completeSurvey(
                StudentRole.BACKEND,
                List.of("Spring"),
                List.of("게시판 구현"),
                true,
                List.of("stu2402"),
                new UserPersonalityScore(4.0, 5.0, 3.0, 2.0, 1.0),
                new UserDevelopmentScore(5.0, 4.0, 3.0, 2.0, 1.0)
        );

        String json = objectMapper.writeValueAsString(AiStudentPayloadDto.from(user));

        assertThat(json).contains("\"user_id\":\"stu2401\"");
        assertThat(json).contains("\"preferred_members\":[\"stu2402\"]");
        assertThat(json).contains("\"ideaPlanning\":4.0");
        assertThat(json).contains("\"responsibility\":4.0");
        assertThat(json).contains("\"roleFlexibility\":3.0");
        assertThat(json).contains("\"collaboration\":3.0");
        assertThat(json).contains("\"timePressure\":2.0");
        assertThat(json).contains("\"flexibility\":2.0");
        assertThat(json).contains("\"staminaFocus\":1.0");
        assertThat(json).contains("\"emotionalStability\":1.0");
        assertThat(json).contains("\"completionQuality\":3.0");
        assertThat(json).contains("\"learningAbility\":3.0");
        assertThat(json).contains("\"presentation\":2.0");
        assertThat(json).contains("\"planning\":2.0");
    }
}
