package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "chat_read_status",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_id", "user_id"})
)
public class ChatReadStatus extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private ChatChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime lastReadAt;

    @Builder
    public ChatReadStatus(ChatChannel channel, User user, LocalDateTime lastReadAt) {
        this.channel = channel;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }

    public void updateLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}
