package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {

    // 동일 토큰 재등록 시 기존 row를 재사용하기 위해 토큰 값으로 조회합니다.
    Optional<UserFcmToken> findByToken(String token);

    // 특정 사용자에게 연결된 모든 토큰을 조회해 일괄 발송 대상으로 사용합니다.
    List<UserFcmToken> findAllByUserUserId(String userId);

    // 로그아웃 시 사용자의 등록 토큰을 한 번에 제거해 이후 불필요한 푸시를 막습니다.
    void deleteAllByUserUserId(String userId);
}
