package com.capteam.gaobackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseTimeEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 1024)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder
    public RefreshToken(String userId, String token, Instant expiresAt) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("refresh token 사용자 ID가 필요합니다.");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("refresh token 값이 필요합니다.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("refresh token 만료 시간이 필요합니다.");
        }
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }
}
