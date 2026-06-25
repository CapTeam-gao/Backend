package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamAssignmentNoticeService {

    private final TeamRepository teamRepository;
    private final TeamUserRepository teamUserRepository;
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

        adminNoticeService.createTeamAssignmentNotice(grade, buildContent(grade, approvedTeams));
    }

    String buildContent(Grade grade, List<Team> teams) {
        StringBuilder content = new StringBuilder()
                .append("# 캡스톤 ")
                .append(toGradeLabel(grade))
                .append(" 팀 배정 결과 안내\n\n")
                .append("캡스톤 팀 배정이 완료되었습니다.\n")
                .append("아래 팀 목록에서 본인이 배정된 팀과 역할을 반드시 확인하세요.\n")
                .append("확인 후 팀 채팅방에서 프로젝트 주제와 역할 분담을 논의하면 됩니다.\n\n")
                .append("## 확인 사항\n")
                .append("- 본인의 팀, 역할, 팀장 여부를 먼저 확인하세요.\n")
                .append("- 팀 채팅방에서 팀원들과 프로젝트 주제를 정리하세요.\n")
                .append("- 프로젝트 기획서는 팀원 협의 후 CapTeam에서 작성하세요.\n")
                .append("- 캡스톤 일지는 안내된 양식에 맞춰 CapTeam에 작성하세요.\n")
                .append("- 공지 미확인으로 생기는 불이익은 본인 및 각 팀 책임입니다.\n\n")
                .append("---\n\n")
                .append("## 팀 목록\n");

        for (Team team : teams) {
            List<TeamUser> members = leaderFirst(teamUserRepository.findByTeamId(team.getId()));

            content.append('\n')
                    .append("### ")
                    .append(team.getTeamName())
                    .append('\n')
                    .append(formatMembers(members))
                    .append('\n')
                    .append(formatRoleSummary(members))
                    .append("\n\n---\n");
        }

        return content.append("\n팀 배정과 관련된 문의가 있다면 담당 선생님께 문의하세요.")
                .toString();

    }

    private List<TeamUser> leaderFirst(List<TeamUser> members) {
        return members.stream()
                .sorted(Comparator.comparing(member -> member.getLeaderRole() == LeaderRole.LEADER ? 0 : 1))
                .toList();
    }

    private String formatMembers(List<TeamUser> members) {
        return members.stream()
                .map(member -> member.getUser().getName()
                        + (member.getLeaderRole() == LeaderRole.LEADER ? " 팀장" : ""))
                .collect(Collectors.joining(" · "));
    }

    private String formatRoleSummary(List<TeamUser> members) {
        Map<StudentRole, Long> roleCounts = members.stream()
                .filter(member -> member.getStudentRole() != null)
                .collect(Collectors.groupingBy(
                        TeamUser::getStudentRole,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        return roleCounts.entrySet().stream()
                .map(entry -> toRoleLabel(entry.getKey()) + " " + entry.getValue() + "명")
                .collect(Collectors.joining(" · "));
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
