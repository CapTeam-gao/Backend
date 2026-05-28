package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamProjectRepository extends JpaRepository<TeamProject,Long> {

    Optional<TeamProject> findByTeamId(Long teamId);
}
