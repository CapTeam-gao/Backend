package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationLogPersistenceService {

    private final NotificationLogRepository notificationLogRepository;

    // 원본 데이터 트랜잭션이 커밋된 뒤 실행되는 FCM 콜백에서도 로그가 반드시 저장되도록
    // 독립된 새 트랜잭션을 열어 발송 이력을 영속화합니다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationLog save(NotificationLog notificationLog) {
        return notificationLogRepository.save(notificationLog);
    }
}
