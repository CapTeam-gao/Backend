package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatFileUploadResponseDto {

    // 프론트가 채팅 메시지의 fileUrl에 넣어 보낼 파일 접근 주소입니다.
    private String fileUrl;

    // 사용자가 업로드한 원래 파일명입니다. 화면에 파일 이름을 보여줄 때 사용할 수 있습니다.
    private String originalFileName;

    // 채팅 메시지 요청의 fileName에 그대로 넣기 좋은 표시용 파일명입니다.
    private String fileName;

    // 서버 저장소 안에서 충돌을 피하려고 UUID를 붙인 실제 저장 파일명입니다.
    private String storedFileName;

    private String contentType;
    private long size;
}
