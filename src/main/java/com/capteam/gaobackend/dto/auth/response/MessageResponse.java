package com.capteam.gaobackend.dto.auth.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    // 단순 성공/안내 메시지를 내려주는 필드입니다.
    private String message;
}
