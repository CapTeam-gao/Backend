package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
