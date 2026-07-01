package com.capteam.gaobackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.file")
public record ChatFileStorageProperties(
        String storageType,
        String localRoot
) {

    public ChatFileStorageProperties {
        if (storageType == null || storageType.isBlank()) {
            storageType = "s3";
        }
        if (localRoot == null || localRoot.isBlank()) {
            localRoot = "/tmp/gao-chat-files";
        }
    }

    public boolean isLocal() {
        return "local".equalsIgnoreCase(storageType);
    }
}
