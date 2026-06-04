package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamRecommendationReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRecommendationReasonRepository extends JpaRepository<TeamRecommendationReason, Long> {

    // 특정 추천안의 모든 배정 이유 조회
    List<TeamRecommendationReason> findByRecommendationId(Long recommendationId);

    // 특정 추천안의 배정 이유 전체 삭제 (추천안 재생성 시 사용)
    void deleteByRecommendationId(Long recommendationId);
}
