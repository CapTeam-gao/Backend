package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.TeamMatchingVersion;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.TeamMatchingVersionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMatchingVersionRepository extends JpaRepository<TeamMatchingVersion, Long> {

    // 학년별 버전 목록을 최신 번호 순으로 보여주기 위해 사용합니다.
    @Query("""
            select version
            from TeamMatchingVersion version
            where version.grade = :grade
            order by version.versionNumber desc
            """)
    List<TeamMatchingVersion> findByGradeOrderByVersionNumberDesc(@Param("grade") Grade grade);

    // 다음 버전 번호를 계산할 때 최신 버전 1건만 빠르게 조회합니다.
    @Query("""
            select version
            from TeamMatchingVersion version
            where version.grade = :grade
            order by version.versionNumber desc
            """)
    List<TeamMatchingVersion> findLatestByGrade(@Param("grade") Grade grade, Pageable pageable);

    // 현재 적용 중인 버전을 찾거나 적용 전환 대상을 좁히는 데 사용합니다.
    List<TeamMatchingVersion> findByGradeAndStatus(Grade grade, TeamMatchingVersionStatus status);

    // 비동기 매칭 작업이 저장한 버전을 jobId로 찾아 프론트 폴링 응답에 versionId를 내려줄 때 사용합니다.
    Optional<TeamMatchingVersion> findByJobId(String jobId);

    default Optional<TeamMatchingVersion> findFirstByGradeOrderByVersionNumberDesc(Grade grade) {
        return findLatestByGrade(grade, Pageable.ofSize(1)).stream().findFirst();
    }
}
