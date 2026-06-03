package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.NoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {

    boolean existsByNoticeIdAndUserUserId(Long noticeId, String userId);

    long countByUserUserId(String userId);
}
