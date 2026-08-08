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

    // 채널 상단에 고정된 메시지 id입니다. 메시지 엔티티 참조 대신 id만 저장해서,
    // 메시지가 삭제될 때 이 값만 지우면 되고 별도 FK 제약을 관리할 필요가 없습니다.
    @Column(name = "pinned_message_id")
    private Long pinnedMessageId;

    @Builder
    public ChatChannel(ChatRoom chatRoom, String channelName, User createdBy) {
        this.chatRoom = chatRoom;
        this.channelName = channelName;
        this.createdBy = createdBy;
    }

    public void updateName(String channelName) {
        this.channelName = channelName;
    }

    public void pinMessage(Long messageId) {
        this.pinnedMessageId = messageId;
    }

    public void unpinMessage() {
        this.pinnedMessageId = null;
    }
}
