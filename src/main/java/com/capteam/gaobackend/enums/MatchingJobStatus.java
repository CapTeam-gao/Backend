package com.capteam.gaobackend.enums;

public enum MatchingJobStatus {
    // 실행 대기 중
    QUEUED,
    // AI 서버 호출 및 결과 처리 중
    RUNNING,
    // 추천안 DB 저장 중이며 더 이상 취소할 수 없는 상태
    COMPLETING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
