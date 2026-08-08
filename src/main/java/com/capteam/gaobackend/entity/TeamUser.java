package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "team_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_team_members_user_id",
                columnNames = "user_id"
        )
)
public class TeamUser extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_role", nullable = false)
    private StudentRole studentRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "leader_role",nullable = false)
    private LeaderRole leaderRole;

    // 팀 확정 후 팀원이 스스로 적어두는 담당 업무입니다. 본인만 수정할 수 있습니다.
    @Column(name = "assigned_task", length = 500)
    private String assignedTask;

    @Builder
    public TeamUser(Team team, User user, StudentRole studentRole, LeaderRole leaderRole) {
        this.team = team;
        this.user = user;
        this.studentRole = studentRole;
        this.leaderRole = leaderRole;
    }

    public void updateTeamMember(Team team, StudentRole studentRole, LeaderRole leaderRole) {
        this.team = team;
        this.studentRole = studentRole;
        this.leaderRole = leaderRole;
    }

    public void updateAssignedTask(String assignedTask) {
        this.assignedTask = assignedTask;
    }
}
