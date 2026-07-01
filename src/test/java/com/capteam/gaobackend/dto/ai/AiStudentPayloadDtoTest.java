package com.capteam.gaobackend.dto.ai;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiStudentPayloadDtoTest {

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
}
