package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.ChatReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatReadStatusRepository extends JpaRepository<ChatReadStatus, Long> {

    Optional<ChatReadStatus> findByChannelIdAndUserUserId(Long channelId, String userId);
    void deleteByChannelId(Long channelId);
}
