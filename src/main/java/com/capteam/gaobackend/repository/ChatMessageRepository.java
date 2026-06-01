package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChannelIdOrderByCreatedAtAsc(Long channelId);   //채널 아이디로 메시지 찾아서 과거순으로
    Page<ChatMessage> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);
    Optional<ChatMessage> findTopByChannelIdOrderByCreatedAtDesc(Long channelId);
    long countByChannelIdAndSenderUserIdNot(Long channelId, String userId);
    long countByChannelIdAndCreatedAtAfterAndSenderUserIdNot(Long channelId, LocalDateTime createdAt, String userId);
    void deleteByChannelId(Long channelId);
}
