package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.service.ChatFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class ChatFileController {

    private final ChatFileStorageService chatFileStorageService;

    @GetMapping("/chat-files/{channelId}/{fileName:.+}")
    public ResponseEntity<Void> getChatFile(
            @PathVariable Long channelId,
            @PathVariable String fileName,
            Authentication authentication
    ) {
        URI downloadUri = URI.create(
                chatFileStorageService.createDownloadUrl(channelId, authentication.getName(), fileName)
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(downloadUri)
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
