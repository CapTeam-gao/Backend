package com.capteam.gaobackend.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotBlank String bucket,
        @NotBlank String region,
        String keyPrefix,
        @NotNull Duration presignedUrlDuration
) {
}
