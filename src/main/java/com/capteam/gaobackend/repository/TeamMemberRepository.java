package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamUser, Long> {

    // 특정 팀의 멤버 목록 조회
    List<TeamUser> findByTeamId(Long teamId);
}
