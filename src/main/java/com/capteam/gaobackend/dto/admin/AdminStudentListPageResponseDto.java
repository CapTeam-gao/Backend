package com.capteam.gaobackend.dto.admin;

import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.Grade;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminStudentListPageResponseDto {

    // 관리자 학생 관리 화면에 표시할 전체 학생 수를 내려주는 필드입니다.
    private long totalStudentCount;

    // 관리자 학생 관리 화면에 표시할 2학년 학생 수를 내려주는 필드입니다.
    private long grade2StudentCount;

    // 관리자 학생 관리 화면에 표시할 3학년 학생 수를 내려주는 필드입니다.
    private long grade3StudentCount;

    // 관리자 학생 관리 화면에 표시할 설문 미제출 학생 수를 내려주는 필드입니다.
    private long surveyNotSubmittedCount;

    // 검색 조건이 반영된 학생 목록을 내려주는 필드입니다.
    private List<AdminStudentListResponseDto> students;

    // 전체 TeamUser 목록과 검색된 학생 목록을 관리자 학생 관리 화면 응답 DTO로 변환하는 기능입니다.
    public static AdminStudentListPageResponseDto of(
            List<TeamUser> allStudents,
            List<AdminStudentListResponseDto> filteredStudents
    ) {
        return AdminStudentListPageResponseDto.builder()
                .totalStudentCount(allStudents.size())
                .grade2StudentCount(countByGrade(allStudents, Grade.GRADE_2))
                .grade3StudentCount(countByGrade(allStudents, Grade.GRADE_3))
                .surveyNotSubmittedCount(allStudents.stream()
                        .filter(teamUser -> !teamUser.getUser().isSurveyCompleted())
                        .count())
                .students(filteredStudents)
                .build();
    }

    // 특정 학년에 해당하는 학생 수를 계산하는 기능입니다.
    private static long countByGrade(List<TeamUser> students, Grade grade) {
        return students.stream()
                .filter(teamUser -> teamUser.getUser().getGrade() == grade)
                .count();
    }
}
