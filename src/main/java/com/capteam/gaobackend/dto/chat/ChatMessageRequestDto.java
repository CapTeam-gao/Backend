package com.capteam.gaobackend.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatMessageRequestDto {

    // 사용자가 직접 보내는 메시지
    // channelId는 URL(/pub/chat/{channelId}/send)에 이미 있으므로 body에는 메시지만 둡니다.
    private String message;

    // 파일 첨부 메시지일 때 사용하는 파일 접근 URL입니다.
    // 예) 프론트가 먼저 파일 업로드 API로 파일을 올리고, 응답으로 받은 URL을 여기에 담아 WebSocket으로 보냅니다.
    private String fileUrl;

    // 파일 URL만 있으면 화면에 파일명/용량 표시가 어렵습니다.
    // 업로드 API 응답값을 그대로 넣어 보내면 채팅 메시지에도 같이 저장됩니다.
    private String fileName;

    // 첨부 파일의 MIME 타입을 저장하기 위해 받는 필드입니다.
    private String fileType;

    // 첨부 파일 크기를 byte 단위로 저장하기 위해 받는 필드입니다.
    private Long fileSize;
}
