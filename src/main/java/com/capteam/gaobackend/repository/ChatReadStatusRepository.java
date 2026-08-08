package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.ChatReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatReadStatusRepository extends JpaRepository<ChatReadStatus, Long> {

    Optional<ChatReadStatus> findByChannelIdAndUserUserId(Long channelId, String userId);
    void deleteByChannelId(Long channelId);

    // 특정 메시지 도착 시점 이후까지 읽은(=해당 메시지를 읽은) 발신자 제외 팀원 수를 세는 기능입니다.
    long countByChannelIdAndUserUserIdNotAndLastReadAtGreaterThanEqual(
            Long channelId,
            String userId,
            LocalDateTime lastReadAt
    );
}
