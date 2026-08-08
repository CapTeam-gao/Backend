package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminStudentSearchResponseDto {

    // 직접 팀 구성에서 학생을 식별하고 저장할 때 사용할 고유 ID입니다.
    private String userId;

    // 직접 팀 구성 검색 결과에 표시할 학생 이름입니다.
    private String name;

    // 직접 팀 구성 대상 학년입니다.
    private Grade grade;

    // 학생이 설문에서 선택한 희망 직군입니다.
    private StudentRole studentRole;

    // 직접 팀 구성 화면에서 바로 표시할 수 있는 직군 한글명입니다.
    private String studentRoleLabel;

    // User 엔티티를 직접 팀 구성 검색 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentSearchResponseDto from(User user) {
        return AdminStudentSearchResponseDto.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .grade(user.getGrade())
                .studentRole(user.getStudentRole())
                .studentRoleLabel(toRoleLabel(user.getStudentRole()))
                .build();
    }

    private static String toRoleLabel(StudentRole studentRole) {
        if (studentRole == null) {
            return null;
        }

        return switch (studentRole) {
            case FRONTEND -> "프론트엔드";
            case BACKEND -> "백엔드";
            case AI -> "AI";
            case APP -> "앱";
            case DESIGN -> "디자인";
            case DEVOPS -> "DevOps";
            case GAME -> "게임";
            case FULLSTACK -> "풀스택";
            case SECURITY -> "보안";
        };
    }
}
