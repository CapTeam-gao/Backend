package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.chat.ChatFileDownloadUrlResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.ChatFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RestController
@RequiredArgsConstructor
public class ChatFileController {

    private final ChatFileStorageService chatFileStorageService;

    @GetMapping("/chat-files/{channelId}/{fileName:.+}")
    public ResponseEntity<StreamingResponseBody> getChatFile(
            @PathVariable Long channelId,
            @PathVariable String fileName,
            Authentication authentication
    ) {
        if (chatFileStorageService.isLocalStorage()) {
            ChatFileStorageService.LocalStoredFile localFile =
                    chatFileStorageService.getLocalFile(channelId, authentication.getName(), fileName);
            MediaType mediaType = localFile.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(localFile.contentType());

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                            .filename(localFile.originalFileName(), StandardCharsets.UTF_8)
                            .build()
                            .toString())
                    .body(outputStream -> {
                        try (var inputStream = Files.newInputStream(localFile.path())) {
                            inputStream.transferTo(outputStream);
                        }
                    });
        }

        ChatFileStorageService.S3StoredFile s3File =
                chatFileStorageService.getS3File(channelId, authentication.getName(), fileName);
        MediaType mediaType = s3File.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(s3File.contentType());
        StreamingResponseBody responseBody = outputStream -> {
            try (var inputStream = s3File.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(s3File.originalFileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString());

        if (s3File.contentLength() != null) {
            responseBuilder.contentLength(s3File.contentLength());
        }

        return responseBuilder.body(responseBody);
    }

    @GetMapping("/api/chat/channels/{channelId}/files/{fileName:.+}/download-url")
    public ResponseEntity<ApiResponse<ChatFileDownloadUrlResponseDto>> getChatFileDownloadUrl(
            @PathVariable Long channelId,
            @PathVariable String fileName,
            Authentication authentication
    ) {
        String downloadUrl = chatFileStorageService.createDownloadUrl(channelId, authentication.getName(), fileName);
        return ApiResponse.ok(ChatFileDownloadUrlResponseDto.builder()
                .downloadUrl(downloadUrl)
                .build());
    }
}
