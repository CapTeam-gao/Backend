package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team,Long> {
}
