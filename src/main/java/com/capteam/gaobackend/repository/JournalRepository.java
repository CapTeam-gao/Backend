package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.enums.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    List<Journal> findAllByOrderByDateDesc();

    boolean existsByTeamIdAndDate(Long teamId, LocalDate date);

    Optional<Journal> findByTeamIdAndDate(Long teamId, LocalDate date);

    List<Journal> findByTeamIdOrderByDateDesc(Long teamId);

    List<Journal> findByDate(LocalDate date);

    @Query("select count(distinct j.team.id) from Journal j where j.date = :date and j.status = :status")
    long countDistinctTeamByDateAndStatus(
            @Param("date") LocalDate date,
            @Param("status") JournalStatus status
    );
}
