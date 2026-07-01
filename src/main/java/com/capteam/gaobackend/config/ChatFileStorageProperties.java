package com.capteam.gaobackend.config;

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
