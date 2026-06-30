package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.service.ChatFileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatFileControllerTest {

    @Mock
    private ChatFileStorageService chatFileStorageService;

    @Test
    void redirectsStableFilePathToPresignedS3UrlWithoutCaching() {
        when(chatFileStorageService.createDownloadUrl(3L, "student1", "file.pdf"))
                .thenReturn("https://private-chat-bucket.s3.amazonaws.com/chat/file.pdf?X-Amz-Signature=test");
        ChatFileController controller = new ChatFileController(chatFileStorageService);

        var authentication = new UsernamePasswordAuthenticationToken("student1", null);
        var response = controller.getChatFile(3L, "file.pdf", authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).hasToString(
                "https://private-chat-bucket.s3.amazonaws.com/chat/file.pdf?X-Amz-Signature=test");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
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
