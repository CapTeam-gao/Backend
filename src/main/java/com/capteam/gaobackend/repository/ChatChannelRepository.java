package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.ChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, Long> {

    List<ChatChannel> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    void deleteByChatRoomId(Long chatRoomId);
}
