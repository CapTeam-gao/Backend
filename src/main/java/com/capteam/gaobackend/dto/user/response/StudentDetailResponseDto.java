package com.capteam.gaobackend.dto.user.response;


import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.enums.StudentLevel;
import com.capteam.gaobackend.enums.StudentRole;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class StudentDetailResponseDto {
    // 학생 상세 조회 대상 userId를 내려주는 필드입니다.
    private String userId;

    // 학생의 팀 내 개발 역할을 내려주는 필드입니다.
    private StudentRole studentRole;

    // 학생의 기술 스택 목록을 내려주는 필드입니다.
    private List<String> skill;

    // 학생의 구현 경험 목록을 내려주는 필드입니다.
    private List<String> experience;

    // 학생이 소속된 팀 이름을 내려주는 필드입니다.
    private String teamName;

    // AI가 생성한 학생 분석 설명을 내려주는 필드입니다.
    private String analysisResult;

    // AI 분석 실력 수준을 내려주는 필드입니다. 상중하
    private StudentLevel studentLevel;

    // TeamUser와 UserAnalysis 엔티티를 학생 상세 응답 DTO로 변환하는 기능입니다.
    public static StudentDetailResponseDto from(TeamUser teamUser, UserAnalysis userAnalysis) {
        return StudentDetailResponseDto.builder()
                .userId(teamUser.getUser().getUserId())
                .studentRole(teamUser.getStudentRole())
                .skill(teamUser.getUser().getSkill())
                .experience(teamUser.getUser().getExperience())
                .teamName(teamUser.getTeam().getTeamName() != null ? teamUser.getTeam().getTeamName() : null)
                .analysisResult(resolveAnalysisResult(userAnalysis))
                .studentLevel(userAnalysis.getStudentLevel())
                .build();
    }

    // 과거 매칭 로직이 실력 등급을 분석 설명에 저장한 데이터는 화면에 노출하지 않습니다.
    private static String resolveAnalysisResult(UserAnalysis userAnalysis) {
        if (userAnalysis == null || userAnalysis.getAnalysisResult() == null) {
            return null;
        }

        String analysisResult = userAnalysis.getAnalysisResult().trim();
        return switch (analysisResult) {
            case "상", "중", "하", "높음", "낮음" -> null;
            default -> analysisResult;
        };
    }
}
