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

    private final NoticeRepository noticeRepository;
    private final NoticeReadRepository noticeReadRepository;
    private final UserRepository userRepository;

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

    public boolean hasUnreadNotice(String userId) {
        long noticeCount = noticeRepository.count();
        if (noticeCount == 0) {
            return false;
        }

        return noticeReadRepository.countByUserUserId(userId) < noticeCount;
    }
}
