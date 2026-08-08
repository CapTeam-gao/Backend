package com.capteam.gaobackend.dto.chat;

import com.capteam.gaobackend.entity.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponseDto {

    // 메시지 고유 id를 내려주는 필드입니다.
    private Long id;

    // 메시지가 속한 채널 id를 내려주는 필드입니다.
    private Long channelId;

    // 메시지를 보낸 사용자 id를 내려주는 필드입니다.
    private String senderId;

    // 메시지를 보낸 사용자 이름을 내려주는 필드입니다.
    private String senderName;

    // 텍스트 메시지 내용을 내려주는 필드입니다.
    private String message;

    // 첨부 파일 접근 URL을 내려주는 필드입니다.
    private String fileUrl;

    // 첨부 파일 표시용 이름을 내려주는 필드입니다.
    private String fileName;

    // 첨부 파일 MIME 타입을 내려주는 필드입니다.
    private String fileType;

    // 첨부 파일 크기를 byte 단위로 내려주는 필드입니다.
    private Long fileSize;

    // 메시지 생성 시각을 내려주는 필드입니다.
    private LocalDateTime createdAt;

    // 메시지 마지막 수정 시각을 내려주는 필드입니다.
    private LocalDateTime updatedAt;

    // 채널 인원(발신자 제외) 중 이 메시지를 읽은 인원 수를 내려주는 필드입니다.
    private long readCount;

    // ChatMessage 엔티티를 프론트 응답 DTO로 변환하는 기능입니다. readCount를 모르는 호출부(마지막 메시지
    // 미리보기 등)를 위해 기본값 0을 쓰는 오버로드입니다.
    public static ChatMessageResponseDto from(ChatMessage chatMessage) {
        return from(chatMessage, 0);
    }

    public static ChatMessageResponseDto from(ChatMessage chatMessage, long readCount) {
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
                .updatedAt(chatMessage.getUpdatedAt())
                .readCount(readCount)
                .build();
    }
}
