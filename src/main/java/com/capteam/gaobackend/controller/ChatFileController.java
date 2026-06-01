package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.service.ChatFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatFileController {

    private final ChatFileStorageService chatFileStorageService;

    @GetMapping("/chat-files/{fileName:.+}")
    public ResponseEntity<Resource> getChatFile(@PathVariable String fileName) {
        // 업로드된 채팅 파일을 프론트에서 링크나 이미지로 바로 열 때 쓰는 endpoint입니다.
        // 실제 파일은 DB가 아니라 chat.file.upload-dir 폴더에서 읽어옵니다.
        return ResponseEntity.ok(chatFileStorageService.loadFile(fileName));
    }
}
