package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRecommendationRepository extends JpaRepository<TeamRecommendation, Long> {

    // 특정 학년의 추천안 목록 조회
    List<TeamRecommendation> findByGrade(Grade grade);

    // 특정 학년 + 특정 상태의 추천안 목록 조회
    List<TeamRecommendation> findByGradeAndStatus(Grade grade, RecommendationStatus status);

    // 특정 상태의 추천안 목록 조회
    List<TeamRecommendation> findByStatus(RecommendationStatus status);
}
