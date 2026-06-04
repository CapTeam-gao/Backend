package com.capteam.gaobackend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Setter
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    // API 요청 성공 여부를 내려주는 필드입니다.
    private boolean success;

    // API 처리 결과 메시지를 내려주는 필드입니다.
    private String message;

    // 실제 응답 데이터를 담아 내려주는 필드입니다.
    private T data;

    // 성공 응답을 HTTP 200과 함께 data까지 담아 반환하는 기능입니다.
    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(new ApiResponse<>(true, "success", data));
    }

    // 성공 메시지만 HTTP 200으로 반환하는 기능입니다.
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message) {
        return ResponseEntity.ok(new ApiResponse<>(true, "success", null));
    }

    // 지정한 HTTP 상태 코드와 메시지를 담아 응답하는 기능입니다.
    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null));
    }
}
