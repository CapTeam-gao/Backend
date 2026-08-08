package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.service.ChatFileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFileControllerTest {

    @Mock
    private ChatFileStorageService chatFileStorageService;

    @Test
    void streamsStableFilePathThroughBackendWithoutCaching() {
        ResponseInputStream<GetObjectResponse> inputStream = mock(ResponseInputStream.class);
        when(chatFileStorageService.getS3File(3L, "student1", "file.pdf"))
                .thenReturn(new ChatFileStorageService.S3StoredFile(
                        inputStream,
                        "file.pdf",
                        "application/pdf",
                        1024L
                ));
        ChatFileController controller = new ChatFileController(chatFileStorageService);

        var authentication = new UsernamePasswordAuthenticationToken("student1", null);
        var response = controller.getChatFile(3L, "file.pdf", authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(1024L);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isInstanceOf(StreamingResponseBody.class);
    }

    @Test
    void returnsPresignedDownloadUrlAsJson() {
        when(chatFileStorageService.createDownloadUrl(3L, "student1", "file.pdf"))
                .thenReturn("https://private-chat-bucket.s3.amazonaws.com/chat/file.pdf?X-Amz-Signature=test");
        ChatFileController controller = new ChatFileController(chatFileStorageService);

        var authentication = new UsernamePasswordAuthenticationToken("student1", null);
        var response = controller.getChatFileDownloadUrl(3L, "file.pdf", authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getDownloadUrl())
                .isEqualTo("https://private-chat-bucket.s3.amazonaws.com/chat/file.pdf?X-Amz-Signature=test");
    }
}
