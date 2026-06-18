package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamUserRepository extends JpaRepository<TeamUser,Long> {
    Optional<TeamUser> findByUserUserId(String userId); // 유저 아이디로 팀원 조회
    List<TeamUser> findAllByUserUserId(String userId);
    List<TeamUser> findByTeamId(Long teamId);           // 특정 팀의 팀원 목록 조회
    boolean existsByUserUserId(String userId);
    boolean existsByTeamIdAndUserUserId(Long teamId, String userId);
}
