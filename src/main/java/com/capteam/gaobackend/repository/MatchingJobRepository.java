package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.MatchingJob;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.MatchingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface MatchingJobRepository extends JpaRepository<MatchingJob, String> {

    boolean existsByGradeAndStatusIn(Grade grade, Collection<MatchingJobStatus> statuses);

    Optional<MatchingJob> findFirstByStatusInOrderByCreatedAtAsc(Collection<MatchingJobStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MatchingJob> findWithLockById(String id);
}
