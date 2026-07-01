package com.capteam.gaobackend.service;

import com.capteam.gaobackend.config.ChatFileStorageProperties;
import com.capteam.gaobackend.config.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private ChatAccessService chatAccessService;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    @TempDir
    private Path tempDirectory;

    private ChatFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new ChatFileStorageService(
                s3Client,
                s3Presigner,
                chatAccessService,
                new S3Properties("private-chat-bucket", "ap-northeast-2", "chat", Duration.ofMinutes(10)),
                new ChatFileStorageProperties("s3", tempDirectory.toString())
        );
    }

    @Test
    void uploadsToPrivateS3AndReturnsStableFileUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "team plan.pdf", "application/pdf", "content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        var response = service.upload(3L, "student1", file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();

        assertThat(request.bucket()).isEqualTo("private-chat-bucket");
        assertThat(request.key()).startsWith("chat/channels/3/").endsWith("_team plan.pdf");
        assertThat(request.serverSideEncryptionAsString()).isEqualTo("AES256");
        assertThat(response.getFileUrl()).startsWith("/chat-files/3/").doesNotContain("X-Amz-");
        assertThat(response.getStoredFileName()).endsWith("_team plan.pdf");
        verify(chatAccessService).getAccessibleChannel(3L, "student1");
    }

    @Test
    void uploadsToLocalStorageWithoutAwsCredentials() throws Exception {
        ChatFileStorageService localService = new ChatFileStorageService(
                s3Client,
                s3Presigner,
                chatAccessService,
                new S3Properties("private-chat-bucket", "ap-northeast-2", "chat", Duration.ofMinutes(10)),
                new ChatFileStorageProperties("local", tempDirectory.toString())
        );
        MockMultipartFile file = new MockMultipartFile(
                "file", "team plan.pdf", "application/pdf", "content".getBytes());

        var response = localService.upload(3L, "student1", file);

        Path savedPath = tempDirectory
                .resolve("channels")
                .resolve("3")
                .resolve(response.getStoredFileName());
        assertThat(Files.readString(savedPath)).isEqualTo("content");
        assertThat(response.getFileUrl()).startsWith("/chat-files/3/");
        assertThat(localService.createDownloadUrl(3L, "student1", response.getStoredFileName()))
                .isEqualTo(response.getFileUrl());
        assertThat(localService.getLocalFile(3L, "student1", response.getStoredFileName()).path())
                .isEqualTo(savedPath.toAbsolutePath().normalize());
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        verify(chatAccessService, times(3)).getAccessibleChannel(3L, "student1");
    }

    @Test
    void createsShortLivedPresignedDownloadUrl() throws Exception {
        when(presignedGetObjectRequest.url())
                .thenReturn(URI.create("https://private-chat-bucket.s3.amazonaws.com/chat/file.pdf").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedGetObjectRequest);

        String downloadUrl = service.createDownloadUrl(3L, "student1", "file.pdf");

        assertThat(downloadUrl).isEqualTo("https://private-chat-bucket.s3.amazonaws.com/chat/file.pdf");
        verify(chatAccessService).getAccessibleChannel(3L, "student1");
    }

    @Test
    void rejectsPathTraversalWhenCreatingDownloadUrl() {
        assertThatThrownBy(() -> service.createDownloadUrl(3L, "student1", "../secret.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 파일명입니다.");
    }
}
