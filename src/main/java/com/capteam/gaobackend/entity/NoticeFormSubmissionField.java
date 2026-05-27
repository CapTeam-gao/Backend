package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

// 학생이 폼의 각 필드에 입력한 실제 값을 저장
// NoticeFormSubmission(제출 기록) 하나에 필드 개수만큼 이 엔티티가 생성됨
// ex) "이름" 필드 → value: "김철수", "학번" 필드 → value: "20001"
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 전용 생성자, 외부 직접 생성 방지
@Table(name = "notice_form_submission_fields")
public class NoticeFormSubmissionField extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 여러 입력값이 하나의 제출 기록에 묶임
    @JoinColumn(name = "submission_id", nullable = false) // DB에 submission_id 컬럼으로 저장
    private NoticeFormSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY) // 어떤 필드(label)에 대한 값인지 참조
    @JoinColumn(name = "field_id", nullable = false) // DB에 field_id 컬럼으로 저장
    private NoticeFormField field; // 어드민이 만든 필드 정의 (ex. "이름", "학번")

    @Column(columnDefinition = "TEXT") // 입력값이 길 수 있으니 TEXT 타입
    private String value; // 학생이 실제로 입력한 값

    @Builder
    public NoticeFormSubmissionField(NoticeFormSubmission submission, NoticeFormField field, String value) {
        this.submission = submission;
        this.field = field;
        this.value = value;
    }
}
