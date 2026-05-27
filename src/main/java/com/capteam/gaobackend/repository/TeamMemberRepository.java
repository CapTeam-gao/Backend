package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    // 특정 팀의 멤버 목록 조회
    List<TeamMember> findByTeamId(Long teamId);
}
