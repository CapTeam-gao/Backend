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

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "chat.file")
public record ChatFileStorageProperties(
        @NotBlank String storageType,
        @NotBlank String localRoot
) {

    public boolean isLocal() {
        return "local".equalsIgnoreCase(storageType);
    }
}
