package com.capteam.gaobackend.dto.user;

import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.TeamRole;
import lombok.Builder;

import java.util.List;


@Builder
public class UserInfoResponse {     //마이페이지

//    이름
//    아이디 pk 아니고 학번 pk는 따로 Long으로 있음 참고
//    희망분야
//    기술 스택  리스트
//    구현 경험은 구현 경험, 구현 방식으로 한다.
//    선호팀원 리스트


    private String name;
    private String studentId;   //user 태이블

    private TeamRole teamRole;  //  유저와 팀의 중간 태이블에 있는 팀 역할 이넘 타입 예를 들어 프론트,백앤드

    private List<String> stack; // 기술 스택 여기서부터는 스킬 태이블

    private List<String> implementExperience; //구현 경험 -> 뭐했는지
    private List<String> methodExperience;  //구현 방식 -> 어떻게 기능을 구현했는지

    private List<String> preferred; //선호 팀원 여러명


//    public static UserInfoResponse from(User user, TeamMember teamMember, Skill skill, TeamRecommendation teamRecommendation) {
//
//
//
//        return UserInfoResponse.builder()
//                .name(user.getName())
//                .studentId(user.getStudentId())
//                .teamRole(teamMember != null ? teamMember.getTeamRole().name() : null)
//                .stack(skill != null ? skill.getStack() : List.of())
//                .implementExperience(skill != null ? skill.getImplementExperience() : List.of())
//                .methodExperience(skill != null ? skill.getMethodExperience() : List.of())
//                .preferred(teamRecommendation != null ? teamRecommendation.getPreferred() : List.of())
//                .build();
//    }
}
