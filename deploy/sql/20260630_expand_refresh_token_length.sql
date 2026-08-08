-- refresh token JWT가 잘리지 않도록 저장 컬럼 길이를 명시적으로 보정합니다.
-- 배포 DB가 예전 VARCHAR(255) 스키마로 생성되어 있으면 로그인 직후에도
-- 저장 토큰과 쿠키 토큰이 달라져 재발급 검증이 실패할 수 있습니다.

ALTER TABLE refresh_tokens
    MODIFY COLUMN token VARCHAR(1024) NOT NULL;
