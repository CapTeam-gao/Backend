package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamUserRepository extends JpaRepository<TeamUser,Long> {
    Optional<TeamUser> findByUserUserId(String userId); //유저안에 유저아이디로 찾기
}
