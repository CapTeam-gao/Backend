package com.capteam.gaobackend.exception;

public class MatchingJobCancelledException extends RuntimeException {

    public MatchingJobCancelledException(String jobId) {
        super("취소된 팀 매칭 작업입니다: " + jobId);
    }
}
