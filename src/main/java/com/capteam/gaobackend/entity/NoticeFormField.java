package com.capteam.gaobackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice_form_fields")
public class NoticeFormField extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    private NoticeForm form;

    @Column(nullable = false)
    private String fieldName;       // 필드 이름 (ex. 이름, 학번, 희망파트)

    @Column(nullable = false)
    private int orderIndex;         // 필드 순서

    private boolean required;       // 필수 여부

    @Builder
    public NoticeFormField(NoticeForm form, String fieldName, int orderIndex, boolean required) {
        this.form = form;
        this.fieldName = fieldName;
        this.orderIndex = orderIndex;
        this.required = required;
    }
}
