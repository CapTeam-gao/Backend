package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 학생이 공지의 폼을 제출했다는 기록
// "누가" "어떤 폼에" 제출했는지를 저장함
// 실제 입력값은 NoticeFormSubmissionField에 저장됨
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA가 객체를 만들 때 쓰는 생성자, 외부에서 직접 new 못 하게 막음
@Table(name = "notice_form_submissions")
public class NoticeFormSubmission extends BaseTimeEntity { // BaseTimeEntity: 생성일/수정일 자동 저장

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 id를 1,2,3... 자동으로 올려줌
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 제출이 하나의 폼에 연결됨 (학생 여러명이 같은 폼 제출 가능)
    @JoinColumn(name = "form_id", nullable = false) // DB에 form_id 컬럼으로 저장
    private NoticeForm form;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 제출이 한 유저에 연결될 수 있음 (유저가 여러 폼 제출 가능)
    @JoinColumn(name = "user_id", nullable = false) // DB에 user_id 컬럼으로 저장
    private User user;

    @Builder // NoticeFormSubmission.builder().form(form).user(user).build() 형태로 생성
    public NoticeFormSubmission(NoticeForm form, User user) {
        this.form = form;
        this.user = user;
    }
}
