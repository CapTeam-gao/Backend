package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_form_submission_fields")
public class NoticeFormSubmissionField extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private NoticeFormSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private NoticeFormField field;

    @Column(columnDefinition = "TEXT")
    private String value;   // 학생이 입력한 값

    @Builder
    public NoticeFormSubmissionField(NoticeFormSubmission submission, NoticeFormField field, String value) {
        this.submission = submission;
        this.field = field;
        this.value = value;
    }
}
