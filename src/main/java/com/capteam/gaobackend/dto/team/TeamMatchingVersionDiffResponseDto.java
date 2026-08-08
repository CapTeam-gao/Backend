package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.enums.StudentRole;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamMatchingVersionDiffResponseDto {

    // 팀 이동 또는 역할 변경이 발생한 학생만 모아 화면 diff 노이즈를 줄입니다.
    private List<MovedStudentDto> movedStudents;

    // 변화가 있었던 추천팀만 빠르게 강조 표시할 수 있도록 추천안 id 목록을 따로 내립니다.
    private List<String> changedTeamIds;

    @Getter
    @Builder
    public static class MovedStudentDto {

        // 어떤 학생 변화인지 식별하기 위한 userId입니다.
        private String userId;

        // 관리자 화면에서 바로 읽기 쉽도록 이름도 함께 내려줍니다.
        private String name;

        // 이전 버전의 추천안 id를 문자열로 내려 diff 응답 스키마를 단순화합니다.
        private String fromTeamId;

        // 이전 버전에서 화면에 보였던 팀 이름입니다.
        private String fromTeamName;

        // 새 버전의 추천안 id를 문자열로 내려 클라이언트가 링크를 만들 수 있게 합니다.
        private String toTeamId;

        // 새 버전에서 화면에 보일 팀 이름입니다.
        private String toTeamName;

        // 역할 변화도 diff 핵심이라 이전 역할을 함께 기록합니다.
        private StudentRole fromStudentRole;

        // 새 버전에서 어떤 역할로 바뀌었는지 바로 비교할 수 있게 합니다.
        private StudentRole toStudentRole;
    }
}
