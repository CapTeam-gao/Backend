package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.StudentLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminStudentDetailResponseDtoTest {

    @Test
    void hidesLegacySkillLevelStoredAsAnalysisResult() {
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        UserAnalysis analysis = UserAnalysis.builder()
                .user(user)
                .analysisResult("중")
                .studentLevel(StudentLevel.MIDDLE)
                .build();

        AdminStudentDetailResponseDto response = AdminStudentDetailResponseDto.from(
                user,
                null,
                analysis,
                new UserDevelopmentScore(0.0, 0.0, 0.0, 0.0, 0.0),
                new UserPersonalityScore(0.0, 0.0, 0.0, 0.0, 0.0)
        );

        assertThat(response.getAnalysisResult()).isNull();
        assertThat(response.getStudentLevel()).isEqualTo(StudentLevel.MIDDLE);
    }
}
