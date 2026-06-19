package com.capteam.gaobackend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageUpdateRequestDto {

    // 수정할 텍스트 메시지 내용을 받는 필드입니다.
    @NotBlank
    private String message;
}
