package com.capteam.gaobackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
public record FirebaseProperties(
        boolean enabled,
        String projectId,
        String credentialsJson,
        String credentialsFile
) {
}
