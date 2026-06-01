package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_messages")
public class ChatMessage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChatChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String message;

    // 파일 첨부 메시지일 때 프론트가 접근할 수 있는 파일 URL을 저장합니다.
    // 실제 파일 바이너리는 DB에 넣지 않고 S3/서버 저장소 등에 올린 뒤 URL만 저장하는 방식이 일반적입니다.
    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    private String fileName;
    private String fileType;
    private Long fileSize;


    @Builder
    public ChatMessage(ChatChannel channel, User sender, String message, String fileUrl,
                       String fileName, String fileType, Long fileSize) {
        this.channel = channel;
        this.sender = sender;
        this.message = message;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }
}
