package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 공지에 첨부되는 신청 폼 (껍데기 역할)
// 제목/작성자는 Notice에 이미 있으므로 중복 제거
// 실제 폼 필드(label, 필수여부 등)는 NoticeFormField에서 관리
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 생성자, 외부 직접 생성 방지
@Table(name = "notice_forms")
public class NoticeForm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 id를 1,2,3... 자동으로 올려줌
    private Long id;

    @Builder // NoticeForm.builder().build() 형태로 생성
    public NoticeForm() {}
}
