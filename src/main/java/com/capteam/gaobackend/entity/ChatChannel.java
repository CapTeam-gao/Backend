package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "chat_channels")
public class ChatChannel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private String channelName;     // ex) 프론트엔드, 백엔드, 공통

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;         // 채널 만든 팀원

    @Builder
    public ChatChannel(ChatRoom chatRoom, String channelName, User createdBy) {
        this.chatRoom = chatRoom;
        this.channelName = channelName;
        this.createdBy = createdBy;
    }

    public void updateName(String channelName) {
        this.channelName = channelName;
    }
}
