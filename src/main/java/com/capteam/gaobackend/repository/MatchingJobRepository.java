package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface MatchingJobRepository extends JpaRepository<MatchingJob, String> {

    boolean existsByGradeAndStatusIn(Grade grade, Collection<MatchingJobStatus> statuses);

    Optional<MatchingJob> findFirstByStatusInOrderByCreatedAtAsc(Collection<MatchingJobStatus> statuses);
}
