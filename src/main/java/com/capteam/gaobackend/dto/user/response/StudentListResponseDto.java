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
    private String name;
    private Grade grade;
    private StudentRole studentRole;
    private List<String> skill;
    private String teamName;


    // 새 객체로 만들어서 리턴
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
