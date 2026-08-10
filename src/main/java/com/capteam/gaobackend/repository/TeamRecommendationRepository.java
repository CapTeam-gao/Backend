package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamRecommendation;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRecommendationRepository extends JpaRepository<TeamRecommendation, Long> {

    // 특정 학년의 추천안 목록 조회
    List<TeamRecommendation> findByGrade(Grade grade);

    // 특정 학년 + 특정 상태의 추천안 목록 조회
    List<TeamRecommendation> findByGradeAndStatus(Grade grade, RecommendationStatus status);

    // 특정 상태의 추천안 목록 조회
    List<TeamRecommendation> findByStatus(RecommendationStatus status);

    // 특정 버전 화면 상세 조회에 추천안을 생성 순서대로 노출할 때 사용합니다.
    List<TeamRecommendation> findByMatchingVersionIdOrderByIdAsc(Long matchingVersionId);

    // 버전 apply 시 한 학년의 현재 적용 버전 추천안을 빠르게 찾는 데 사용합니다.
    List<TeamRecommendation> findByMatchingVersionId(Long matchingVersionId);

    // 단건 추천안이 어느 버전에 속했는지 확인할 때 사용합니다.
    Optional<TeamRecommendation> findFirstByMatchingVersionId(Long matchingVersionId);

    // 배치 스트리밍 중 같은 팀(team_update → team_ready)이 다시 도착했을 때 기존 row를
    // 찾아 갱신(upsert)하기 위해 사용합니다.
    Optional<TeamRecommendation> findByMatchingVersionIdAndAiTeamName(Long matchingVersionId, String aiTeamName);
}
