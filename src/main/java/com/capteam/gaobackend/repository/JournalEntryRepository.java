package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByJournalId(Long journalId);

    boolean existsByJournalTeamIdAndJournalDateAndWriterUserId(Long teamId, java.time.LocalDate date, String userId);

    Optional<JournalEntry> findByJournalIdAndWriterUserId(Long journalId, String userId);

    long countByJournalId(Long journalId);
}
