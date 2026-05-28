//package com.capteam.gaobackend.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//
//public interface NoticeFormFieldRepository extends JpaRepository<NoticeFormField, Long> {
//
//    // 특정 폼의 모든 필드를 순서대로 조회
//    List<NoticeFormField> findByFormIdOrderByOrderIndex(Long formId);
//
//    // 특정 폼의 모든 필드 삭제 (폼 필드 전체 교체 시 사용)
//    void deleteByFormId(Long formId);
//}
