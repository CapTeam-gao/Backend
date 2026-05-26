package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 학생이 공지를 읽었다는 기록
// 어드민이 "누가 읽었는지" 조회할 때 사용
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 생성자, 외부 직접 생성 방지
@Table(name = "notice_reads")
public class NoticeRead extends BaseTimeEntity { // BaseTimeEntity의 createdAt = 읽은 시각

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 id를 1,2,3... 자동으로 올려줌
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 읽음 기록이 하나의 공지에 연결됨
    @JoinColumn(name = "notice_id", nullable = false) // DB에 notice_id 컬럼으로 저장
    private Notice notice;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 읽음 기록이 한 유저에 연결될 수 있음
    @JoinColumn(name = "user_id", nullable = false) // DB에 user_id 컬럼으로 저장
    private User user;

    @Builder // NoticeRead.builder().notice(notice).user(user).build() 형태로 생성
    public NoticeRead(Notice notice, User user) {
        this.notice = notice;
        this.user = user;
    }
}
