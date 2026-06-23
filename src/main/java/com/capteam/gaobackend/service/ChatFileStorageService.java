package com.capteam.gaobackend.service;

import com.capteam.gaobackend.config.S3Properties;
import com.capteam.gaobackend.dto.chat.ChatFileUploadResponseDto;
import com.capteam.gaobackend.exception.ChatFileNotFoundException;
import com.capteam.gaobackend.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatFileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final ChatAccessService chatAccessService;
    private final S3Properties s3Properties;

    private static final String PUBLIC_PATH = "/chat-files";
    private static final int MAX_FILE_NAME_LENGTH = 120;

    public ChatFileUploadResponseDto upload(Long channelId, String userId, MultipartFile file) {
        chatAccessService.getAccessibleChannel(channelId, userId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        String safeOriginalFileName = getSafeFileName(originalFileName);
        String storedFileName = UUID.randomUUID() + "_" + safeOriginalFileName;
        String objectKey = buildObjectKey(channelId, storedFileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .contentDisposition(ContentDisposition.inline()
                        .filename(safeOriginalFileName, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return ChatFileUploadResponseDto.builder()
                    .fileUrl(buildFileUrl(channelId, storedFileName))
                    .originalFileName(safeOriginalFileName)
                    .fileName(safeOriginalFileName)
                    .storedFileName(storedFileName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (IOException | SdkException e) {
            throw new FileStorageException("파일 저장소에 파일을 저장하지 못했습니다.", e);
        }
    }

    public String createDownloadUrl(Long channelId, String userId, String storedFileName) {
        chatAccessService.getAccessibleChannel(channelId, userId);
        validateStoredFileName(storedFileName);
        String objectKey = buildObjectKey(channelId, storedFileName);

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ChatFileNotFoundException("채팅 파일을 찾을 수 없습니다.");
            }
            throw new FileStorageException("파일 저장소에서 파일을 확인하지 못했습니다.", e);
        } catch (SdkException e) {
            throw new FileStorageException("파일 저장소에서 파일을 확인하지 못했습니다.", e);
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(s3Properties.presignedUrlDuration())
                .getObjectRequest(getObjectRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (SdkException e) {
            throw new FileStorageException("파일 다운로드 URL을 생성하지 못했습니다.", e);
        }
    }

    private String getSafeFileName(String originalFileName) {
        String fileName = originalFileName == null ? "file" : Paths.get(originalFileName).getFileName().toString();
        fileName = fileName.trim();

        if (fileName.isEmpty()) {
            return "file";
        }

        String safeFileName = fileName
                .replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_");
        return safeFileName.length() <= MAX_FILE_NAME_LENGTH
                ? safeFileName
                : safeFileName.substring(0, MAX_FILE_NAME_LENGTH);
    }

    private String buildFileUrl(Long channelId, String storedFileName) {
        return UriComponentsBuilder.fromPath(PUBLIC_PATH)
                .pathSegment(channelId.toString())
                .pathSegment(storedFileName)
                .build()
                .encode()
                .toUriString();
    }

    private String buildObjectKey(Long channelId, String storedFileName) {
        String keyPrefix = s3Properties.keyPrefix();
        String normalizedPrefix = keyPrefix == null ? "" : keyPrefix.trim().replaceAll("^/+|/+$", "");
        String channelKey = "channels/" + channelId + "/" + storedFileName;
        return normalizedPrefix.isEmpty() ? channelKey : normalizedPrefix + "/" + channelKey;
    }

    private void validateStoredFileName(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()
                || storedFileName.contains("/") || storedFileName.contains("\\")) {
            throw new IllegalArgumentException("잘못된 파일명입니다.");
        }
    }
}
