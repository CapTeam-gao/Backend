package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채널의 메시지를 과거순으로 조회하는 기능입니다.
    List<ChatMessage> findByChannelIdOrderByCreatedAtAsc(Long channelId);

    // 특정 채널의 메시지를 최신순 페이지로 조회하는 기능입니다.
    Page<ChatMessage> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);

    // 특정 채널의 마지막 메시지 1개를 조회하는 기능입니다.
    Optional<ChatMessage> findTopByChannelIdOrderByCreatedAtDesc(Long channelId);

    // 특정 채널에서 내가 보낸 메시지를 제외한 전체 메시지 수를 계산하는 기능입니다.
    long countByChannelIdAndSenderUserIdNot(Long channelId, String userId);

    // 특정 채널에서 기준 시간 이후 내가 보낸 메시지를 제외한 메시지 수를 계산하는 기능입니다.
    long countByChannelIdAndCreatedAtAfterAndSenderUserIdNot(Long channelId, LocalDateTime createdAt, String userId);

    // 특정 채널의 모든 메시지를 삭제하는 기능입니다.
    void deleteByChannelId(Long channelId);
}
