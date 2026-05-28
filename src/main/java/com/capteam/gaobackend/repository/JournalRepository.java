package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.Journal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalRepository extends JpaRepository<Journal, Long> {

    List<Journal> findAllByOrderByDateDesc();
}
