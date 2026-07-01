package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.chat.ChatFileDownloadUrlResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.ChatFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
public class ChatFileController {

    private final ChatFileStorageService chatFileStorageService;

    @GetMapping("/chat-files/{channelId}/{fileName:.+}")
    public ResponseEntity<?> getChatFile(
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
                    .body(new FileSystemResource(localFile.path()));
        }

        URI downloadUri = URI.create(
                chatFileStorageService.createDownloadUrl(channelId, authentication.getName(), fileName)
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(downloadUri)
                .cacheControl(CacheControl.noStore())
                .build();
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
