package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.entity.NoticeRead;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.repository.NoticeReadRepository;
import com.capteam.gaobackend.repository.NoticeRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    // 전체 공지 개수를 확인하고 공지 엔티티를 조회하는 Repository 필드입니다.
    private final NoticeRepository noticeRepository;

    // 사용자별 공지 읽음 기록을 조회/저장하는 Repository 필드입니다.
    private final NoticeReadRepository noticeReadRepository;

    // 공지 읽음 처리 대상 사용자를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 특정 사용자가 특정 공지를 읽음 처리하는 기능입니다.
    @Transactional
    public void markAsRead(Long noticeId, String userId) {
        if (noticeReadRepository.existsByNoticeIdAndUserUserId(noticeId, userId)) {
            return;
        }

        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        noticeReadRepository.save(NoticeRead.builder()
                .notice(notice)
                .user(user)
                .build());
    }

    // 사용자가 아직 읽지 않은 공지가 하나라도 있는지 확인하는 기능입니다.
    public boolean hasUnreadNotice(String userId) {
        return noticeReadRepository.existsUnreadNoticeByUserId(userId);
    }
}
