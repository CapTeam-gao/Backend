package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.enums.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {

    // 학년별 팀 목록 조회
    List<Team> findByGrade(Grade grade);

    // 학년별 팀 수 조회 (1팀, 2팀 번호 자동 생성 시 사용)
    long countByGrade(Grade grade);
}
