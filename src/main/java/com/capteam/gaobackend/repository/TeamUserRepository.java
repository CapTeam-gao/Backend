package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamUserRepository extends JpaRepository<TeamUser,Long> {
}
