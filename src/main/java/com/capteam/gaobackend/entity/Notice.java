package com.capteam.gaobackend.entity;

import com.capteam.gaobackend.enums.Grade;
import jakarta.persistence.*;
import lombok.*;

// 어드민이 작성하는 공지사항
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 생성자, 외부 직접 생성 방지
@Table(name = "notices")
public class Notice extends BaseTimeEntity { // BaseTimeEntity: 생성일/수정일 자동 저장

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 id를 1,2,3... 자동으로 올려줌
    private Long id;

    @Column(nullable = false)
    private String title; // 공지 제목

    @Column(columnDefinition = "TEXT", nullable = false) // 내용이 길 수 있으니 TEXT 타입
    private String content; // 공지 내용

    @ManyToOne(fetch = FetchType.LAZY) // 여러 공지가 한 명의 작성자에 연결됨
    @JoinColumn(name = "author_id", nullable = false) // DB에 author_id 컬럼으로 저장
    private User author; // 공지 작성자 (어드민)

    @Enumerated(EnumType.STRING) // DB에 숫자 대신 "GRADE_1" 같은 문자열로 저장
    private Grade grade; // 대상 학년 (null이면 전체 학년 대상)

    // 공지에 신청 폼이 붙을 수 있음 (null이면 폼 없는 일반 공지)
    // cascade = ALL: 공지 저장/삭제 시 폼도 함께 저장/삭제
    // orphanRemoval = true: 폼 연결이 끊기면(removeForm 호출 시) DB에서도 폼 자동 삭제
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "form_id")
    private NoticeForm form;

    @Builder // Notice.builder().title("...").content("...").author(user).grade(grade).build() 형태로 생성
    public Notice(String title, String content, User author, Grade grade) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.grade = grade;
    }

    // 공지 수정 (제목, 내용, 대상 학년)
    public void update(String title, String content, Grade grade) {
        this.title = title;
        this.content = content;
        this.grade = grade;
    }

    // 폼 연결 (공지에 신청 폼 추가)
    public void attachForm(NoticeForm form) {
        this.form = form;
    }

    // 폼 연결 해제 (orphanRemoval=true 덕분에 DB에서도 폼 자동 삭제됨)
    public void removeForm() {
        this.form = null;
    }
}
