package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.notice.*;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용, 데이터 변경 메서드는 @Transactional 따로 붙임
public class AdminNoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────
    // 공지 목록 조회 (최신순)
    // ──────────────────────────────────────────
    public List<NoticeResponseDto> getNoticeList() {
        return noticeRepository.findAll().stream()
                .map(NoticeResponseDto::from) // 각 Notice를 DTO로 변환
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // 공지 상세 조회
    // ──────────────────────────────────────────
    public NoticeDetailResponseDto getNoticeDetail(Long noticeId) {
        // findById는 Optional을 반환하므로 없으면 예외 던짐
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));
        return NoticeDetailResponseDto.from(notice);
    }

    // ──────────────────────────────────────────
    // 공지 생성
    // ──────────────────────────────────────────
    @Transactional // 데이터를 저장하므로 쓰기 트랜잭션 필요
    public NoticeDetailResponseDto createNotice(NoticeCreateRequestDto dto) {
        User author = getAuthenticatedUser(); // 현재 로그인한 어드민 가져오기

        // 공지 엔티티 생성 (Builder 패턴)
        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(author)
                .important(dto.getImportant())
                .build();

        noticeRepository.save(notice); // DB에 저장

        return NoticeDetailResponseDto.from(notice);
    }

    // ──────────────────────────────────────────
    // 공지 수정
    // ──────────────────────────────────────────
    @Transactional
    public NoticeDetailResponseDto updateNotice(Long noticeId, NoticeUpdateRequestDto dto) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        // setter 대신 엔티티의 update() 메서드로 수정
        notice.update(dto.getTitle(), dto.getContent(), dto.getImportant());

        // @Transactional 덕분에 save() 없이도 변경사항이 자동으로 DB에 반영됨 (더티 체킹)
        return NoticeDetailResponseDto.from(notice);
    }

    // ──────────────────────────────────────────
    // 공지 삭제
    // ──────────────────────────────────────────
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        // 공지 삭제 시 cascade=ALL 덕분에 연결된 NoticeForm도 자동 삭제
        noticeRepository.delete(notice);
    }

    // ──────────────────────────────────────────
    // 공통: 현재 로그인한 유저 가져오기
    // SecurityContext에서 userId를 꺼내서 DB에서 User 조회
    // ──────────────────────────────────────────
    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
