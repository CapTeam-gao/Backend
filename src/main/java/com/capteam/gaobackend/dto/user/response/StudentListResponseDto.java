package com.capteam.gaobackend.dto.user.response;


import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class StudentListResponseDto {    // 학생 전체 조회
    // 학생 이름을 내려주는 필드입니다.
    private String name;

    // 학생 학년을 내려주는 필드입니다.
    private Grade grade;

    // 학생의 팀 내 개발 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // 학생의 기술 스택 목록을 내려주는 필드입니다.
    private List<String> skill;

    // 학생이 소속된 팀 이름을 내려주는 필드입니다.
    private String teamName;


    // TeamUser 엔티티를 학생 목록 응답 DTO로 변환하는 기능입니다.
    public  static StudentListResponseDto from(TeamUser teamUser) {
        return StudentListResponseDto.builder()
                .name(teamUser.getUser().getName())
                .grade(teamUser.getUser().getGrade())
                .studentRole(teamUser.getStudentRole() != null ? teamUser.getStudentRole() : null)  //역할 프론트,백엔드 등
                .skill(teamUser.getUser().getSkill() != null ? teamUser.getUser().getSkill() : null)    //기술 스택
                .teamName(teamUser.getTeam().getTeamName() != null ? teamUser.getTeam().getTeamName() : null) //팀 이름
                .build();
    }
}
