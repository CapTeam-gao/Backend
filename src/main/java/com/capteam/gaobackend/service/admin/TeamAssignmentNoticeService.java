package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamAssignmentNoticeService {

    private static final String INTRODUCTION = """
            캡스톤 팀 배정이 완료되었습니다.
            아래 팀 목록을 확인한 뒤 팀 채팅방과 프로젝트 기획서 작성을 진행해주세요.
            """;

    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
    private final TeamProjectRepository teamProjectRepository;
    private final AdminNoticeService adminNoticeService;

    // 최종 승인된 학년의 전체 팀 정보를 마크다운 공지로 생성하는 기능입니다.
    public void createNotice(Grade grade) {
        List<Team> approvedTeams = teamRepository.findByGrade(grade).stream()
                .filter(team -> team.getStatus() == TeamStatus.APPROVED)
                .sorted(Comparator.comparing(Team::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        if (approvedTeams.isEmpty()) {
            throw new IllegalStateException("공지에 포함할 승인 완료 팀이 없습니다.");
        }

        adminNoticeService.createTeamAssignmentNotice(buildContent(approvedTeams));
    }

    String buildContent(List<Team> teams) {
        StringBuilder content = new StringBuilder(INTRODUCTION)
                .append('\n')
                .append("## 팀 목록\n");

        for (Team team : teams) {
            content.append('\n')
                    .append("### ")
                    .append(resolveTeamName(team))
                    .append('\n')
                    .append("- 학년: ")
                    .append(toGradeLabel(team.getGrade()))
                    .append('\n')
                    .append("- 팀원\n");

            List<TeamUser> members = teamUserRepository.findByTeamId(team.getId()).stream()
                    .sorted(Comparator.comparing(TeamUser::getId, Comparator.nullsLast(Long::compareTo)))
                    .toList();
            for (TeamUser member : members) {
                content.append("  - ")
                        .append(member.getUser().getName())
                        .append(" / ")
                        .append(toRoleLabel(member.getStudentRole()))
                        .append('\n');
            }
        }

        return content.toString().stripTrailing();
    }

    private String resolveTeamName(Team team) {
        return teamProjectRepository.findByTeamId(team.getId())
                .map(TeamProject::getTeamName)
                .filter(teamName -> !teamName.isBlank())
                .orElse(team.getTeamName());
    }

    private String toGradeLabel(Grade grade) {
        return switch (grade) {
            case GRADE_2 -> "2학년";
            case GRADE_3 -> "3학년";
        };
    }

    private String toRoleLabel(StudentRole role) {
        return switch (role) {
            case BACKEND -> "백엔드";
            case FRONTEND -> "프론트엔드";
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
