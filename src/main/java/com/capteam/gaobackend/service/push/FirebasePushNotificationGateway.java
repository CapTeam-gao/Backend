package com.capteam.gaobackend.service.push;

import com.capteam.gaobackend.config.FirebaseProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class FirebasePushNotificationGateway implements PushNotificationGateway {

    private final FirebaseProperties firebaseProperties;
    private volatile FirebaseMessaging firebaseMessaging;

    public FirebasePushNotificationGateway(FirebaseProperties firebaseProperties) {
        this.firebaseProperties = firebaseProperties;
    }

    @Override
    public PushDispatchResult sendToTokens(List<String> tokens, PushMessageRequest request) {
        if (tokens == null || tokens.isEmpty()) {
            return PushDispatchResult.failure(0, "활성 FCM 토큰이 없습니다.");
        }

        FirebaseMessaging messaging = resolveFirebaseMessaging();
        if (messaging == null) {
            return PushDispatchResult.failure(tokens.size(), "Firebase FCM 설정이 비어 있습니다.");
        }

        int successCount = 0;
        List<String> failures = new ArrayList<>();
        for (String token : tokens) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(request.title())
                                .setBody(request.body())
                                .build());

                Map<String, String> data = request.data();
                if (data != null && !data.isEmpty()) {
                    builder.putAllData(data);
                }

                messaging.send(builder.build());
                successCount++;
            } catch (Exception e) {
                log.warn("FCM 푸시 발송 실패 token={}", token, e);
                failures.add(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }

        return new PushDispatchResult(successCount, failures.size(), failures);
    }

    private FirebaseMessaging resolveFirebaseMessaging() {
        if (!firebaseProperties.enabled()) {
            return null;
        }

        FirebaseMessaging current = firebaseMessaging;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (firebaseMessaging != null) {
                return firebaseMessaging;
            }

            try {
                GoogleCredentials credentials = resolveCredentials();
                if (credentials == null) {
                    return null;
                }

                String appName = "gao-fcm-app";
                FirebaseApp app = FirebaseApp.getApps().stream()
                        .filter(existing -> existing.getName().equals(appName))
                        .findFirst()
                        .orElseGet(() -> FirebaseApp.initializeApp(
                                FirebaseOptions.builder()
                                        .setCredentials(credentials)
                                        .setProjectId(firebaseProperties.projectId())
                                        .build(),
                                appName
                        ));
                firebaseMessaging = FirebaseMessaging.getInstance(app);
                return firebaseMessaging;
            } catch (Exception e) {
                log.error("Firebase 초기화에 실패했습니다.", e);
                return null;
            }
        }
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (firebaseProperties.credentialsJson() != null && !firebaseProperties.credentialsJson().isBlank()) {
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(firebaseProperties.credentialsJson().getBytes(StandardCharsets.UTF_8))
            );
        }

        if (firebaseProperties.credentialsFile() != null && !firebaseProperties.credentialsFile().isBlank()) {
            return GoogleCredentials.fromStream(Files.newInputStream(Path.of(firebaseProperties.credentialsFile())));
        }

        return null;
    }
}
