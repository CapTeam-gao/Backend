package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamRecommendationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRecommendationMemberRepository extends JpaRepository<TeamRecommendationMember, Long> {

    // 특정 추천안의 멤버 목록 조회
    List<TeamRecommendationMember> findByRecommendationId(Long recommendationId);

    // 특정 추천안의 특정 유저 멤버 조회 (swap에 사용)
    Optional<TeamRecommendationMember> findByRecommendationIdAndUserUserId(Long recommendationId, String userId);

    // 특정 추천안의 멤버 전체 삭제 (추천안 재생성 시 사용)
    void deleteByRecommendationId(Long recommendationId);
}
