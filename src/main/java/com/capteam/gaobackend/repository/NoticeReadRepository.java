package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.NoticeRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeReadRepository extends JpaRepository<NoticeRead, Long> {

    boolean existsByNoticeIdAndUserUserId(Long noticeId, String userId);

    long countByUserUserId(String userId);

    void deleteByNoticeId(Long noticeId);

    // 사용자가 아직 읽지 않은 공지가 하나라도 있는지 확인하는 기능입니다.
    @Query("""
            select count(n) > 0
            from Notice n
            where not exists (
                select 1
                from NoticeRead nr
                where nr.notice = n
                  and nr.user.userId = :userId
            )
            """)
    boolean existsUnreadNoticeByUserId(@Param("userId") String userId);
}
