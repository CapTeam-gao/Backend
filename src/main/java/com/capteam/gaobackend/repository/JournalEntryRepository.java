package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByJournalId(Long journalId);
}
