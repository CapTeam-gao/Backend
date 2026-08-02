package com.capteam.gaobackend.dto.user.response;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentSearchResponseDto {

    // 선호 팀원 저장에 사용할 학생 고유 ID를 내려주는 필드입니다.
    private String userId;

    // 검색 결과에 표시할 학생 이름을 내려주는 필드입니다.
    private String name;

    // 검색 결과에 표시할 학생 학년을 내려주는 필드입니다.
    private Grade grade;

    // 검색 결과에 표시할 학생 희망 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // User 엔티티를 학생 검색 응답 DTO로 변환하는 기능입니다.
    public static StudentSearchResponseDto from(User user) {
        return StudentSearchResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .grade(user.getGrade())
                .studentRole(user.getStudentRole())
                .build();
    }
}
