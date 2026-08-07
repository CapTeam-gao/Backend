package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    // 동일 사용자에게 같은 대상 알림이 이미 기록됐는지 확인해 중복 발송을 막습니다.
    boolean existsByUserUserIdAndTypeAndTargetId(String userId, NotificationType type, String targetId);

    // 고유 조합으로 기존 이력을 다시 읽어야 할 때 사용합니다.
    Optional<NotificationLog> findByUserUserIdAndTypeAndTargetId(String userId, NotificationType type, String targetId);

    @Query("""
            select nl
            from NotificationLog nl
            where (:type is null or nl.type = :type)
              and (:status is null or nl.status = :status)
              and (:targetId is null or nl.targetId = :targetId)
              and (:userId is null or nl.user.userId = :userId)
            order by nl.sentAt desc, nl.createdAt desc
            """)
    List<NotificationLog> searchLogs(
            @Param("type") NotificationType type,
            @Param("status") NotificationStatus status,
            @Param("targetId") String targetId,
            @Param("userId") String userId
    );
}
