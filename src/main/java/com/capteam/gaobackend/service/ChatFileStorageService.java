package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatFileUploadResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatFileStorageService {

    @Value("${chat.file.upload-dir}")
    private String uploadDir;

    @Value("${chat.file.public-path}")
    private String publicPath;

    private final ChatAccessService chatAccessService;

    public ChatFileUploadResponseDto upload(Long channelId, String userId, MultipartFile file) {
        // 파일 업로드 전에 채널 접근 권한을 먼저 확인합니다.
        // 학생이 다른 팀 채널에 파일 URL을 만들어 붙이는 상황을 막기 위함입니다.
        chatAccessService.getAccessibleChannel(channelId, userId);

        // MultipartFile은 HTTP multipart/form-data로 넘어온 파일 하나를 의미합니다.
        // WebSocket은 실시간 메시지 전달용으로만 쓰고, 파일 바이너리는 이 서비스에서 디스크에 저장합니다.
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        String safeOriginalFileName = getSafeFileName(originalFileName);
        String storedFileName = UUID.randomUUID() + "_" + safeOriginalFileName;

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(storedFileName).normalize();
            if (!targetPath.startsWith(uploadPath)) {
                throw new IllegalArgumentException("잘못된 파일명입니다.");
            }

            file.transferTo(targetPath);

            return ChatFileUploadResponseDto.builder()
                    .fileUrl(buildFileUrl(storedFileName))
                    .originalFileName(safeOriginalFileName)
                    .fileName(safeOriginalFileName)
                    .storedFileName(storedFileName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("파일 저장에 실패했습니다.", e);
        }
    }

    public Resource loadFile(String storedFileName) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(storedFileName).normalize();

            // URL에 ../ 같은 값이 들어와도 업로드 폴더 밖으로 나가지 못하게 막습니다.
            if (!filePath.startsWith(uploadPath)) {
                throw new IllegalArgumentException("잘못된 파일명입니다.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("잘못된 파일 경로입니다.", e);
        }
    }

    private String getSafeFileName(String originalFileName) {
        // 브라우저나 OS에 따라 파일명에 경로가 섞여 들어오는 경우가 있어 파일명만 분리합니다.
        String fileName = originalFileName == null ? "file" : Paths.get(originalFileName).getFileName().toString();
        fileName = fileName.trim();

        if (fileName.isEmpty()) {
            return "file";
        }

        // URL과 파일 시스템에서 문제가 될 수 있는 문자는 '_'로 바꿉니다.
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String buildFileUrl(String storedFileName) {
        String normalizedPublicPath = publicPath.endsWith("/")
                ? publicPath.substring(0, publicPath.length() - 1)
                : publicPath;

        return normalizedPublicPath + "/" + storedFileName;
    }
}
