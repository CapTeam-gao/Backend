package com.capteam.gaobackend.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatFileDownloadUrlResponseDto {

    // 프론트가 이 URL로 이동하면 S3 파일을 내려받을 수 있습니다.
    private String downloadUrl;
}
