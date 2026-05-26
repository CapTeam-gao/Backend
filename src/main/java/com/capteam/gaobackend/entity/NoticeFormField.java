package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 폼 안의 각 입력 필드 정의 (어드민이 + 버튼으로 추가, - 버튼으로 삭제)
// ex) "이름", "학번", "희망 파트" 같은 label 역할
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 생성자, 외부 직접 생성 방지
@Table(name = "notice_form_fields")
public class NoticeFormField extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB가 id를 1,2,3... 자동으로 올려줌
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 필드가 하나의 폼에 속함
    @JoinColumn(name = "form_id", nullable = false) // DB에 form_id 컬럼으로 저장
    private NoticeForm form;

    @Column(nullable = false)
    private String fieldName; // 필드 이름 (ex. "이름", "학번", "희망 파트")

    @Column(nullable = false)
    private int orderIndex; // 필드 표시 순서 (0부터 시작, 작을수록 위에 표시)

    private boolean required; // 필수 입력 여부 (true면 학생이 반드시 입력해야 함)

    @Builder // NoticeFormField.builder().form(form).fieldName("이름").orderIndex(0).required(true).build()
    public NoticeFormField(NoticeForm form, String fieldName, int orderIndex, boolean required) {
        this.form = form;
        this.fieldName = fieldName;
        this.orderIndex = orderIndex;
        this.required = required;
    }

    // 필드 수정 (이름, 순서, 필수여부)
    public void update(String fieldName, int orderIndex, boolean required) {
        this.fieldName = fieldName;
        this.orderIndex = orderIndex;
        this.required = required;
    }
}
