package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponseDto {

    private Long id;
    private Long channelId;
    private String senderId;
    private String senderName;
    private String message;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createdAt;

    public static ChatMessageResponseDto from(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .id(chatMessage.getId())
                .channelId(chatMessage.getChannel().getId())
                .senderId(chatMessage.getSender().getUserId())
                .senderName(chatMessage.getSender().getName())
                .message(chatMessage.getMessage())
                .fileUrl(chatMessage.getFileUrl())
                .fileName(chatMessage.getFileName())
                .fileType(chatMessage.getFileType())
                .fileSize(chatMessage.getFileSize())
                .createdAt(chatMessage.getCreatedAt())
                .build();
    }
}
