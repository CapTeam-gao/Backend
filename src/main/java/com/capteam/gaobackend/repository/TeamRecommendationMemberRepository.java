package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamRecommendationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamRecommendationMemberRepository extends JpaRepository<TeamRecommendationMember, Long> {

    // 특정 추천안의 멤버 목록 조회
    List<TeamRecommendationMember> findByRecommendationId(Long recommendationId);
}
