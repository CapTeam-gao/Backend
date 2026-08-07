package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_fcm_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_fcm_token_value", columnNames = "token")
        }
)
public class UserFcmToken extends BaseTimeEntity {

    // 토큰 삭제 API가 특정 row를 식별할 때 사용하는 PK입니다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 사용자의 브라우저/앱에서 등록된 토큰인지 추적하기 위해 사용자 FK를 보관합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FCM 서버가 실제 푸시 대상을 식별하는 토큰 문자열입니다.
    // 같은 토큰이 여러 사용자에게 중복 연결되면 안 되므로 DB unique 제약을 둡니다.
    @Column(nullable = false, length = 512)
    private String token;

    // 기존 브랜치 테이블의 NOT NULL 제약과 호환되게 유지하는 내부 상태 값입니다.
    // 상세 설계서 계약에는 노출하지 않고, 현재 저장소 스키마를 안전하게 흡수하는 용도로만 씁니다.
    @Column(nullable = false)
    private boolean active;

    // 과거 스키마의 NOT NULL 제약을 만족시키기 위해 마지막 등록 시각을 계속 기록합니다.
    @Column(name = "last_registered_at", nullable = false)
    private LocalDateTime lastRegisteredAt;

    // 과거 스키마에 남아 있는 선택 필드입니다. 현재 2번 설계 범위에서는 사용하지 않습니다.
    @Column(name = "device_type", length = 50)
    private String deviceType;

    // 과거 비활성화 구조와의 호환용 컬럼입니다. 현재는 DELETE 시 row 자체를 삭제하므로 null로 유지합니다.
    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @Builder
    public UserFcmToken(User user, String token, LocalDateTime lastRegisteredAt) {
        this.user = user;
        this.token = token;
        this.active = true;
        this.lastRegisteredAt = lastRegisteredAt;
    }

    // 동일 토큰이 재등록되면 사용자 소유권만 최신 로그인 사용자로 갱신합니다.
    public void reassign(User user, LocalDateTime registeredAt) {
        this.user = user;
        this.active = true;
        this.lastRegisteredAt = registeredAt;
        this.deactivatedAt = null;
    }
}
