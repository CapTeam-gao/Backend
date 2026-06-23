package com.capteam.gaobackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    public DefaultCredentialsProvider awsCredentialsProvider() {
        // 로컬에서는 AWS profile/environment를, EC2에서는 Instance Profile의 임시 자격 증명을 사용합니다.
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(S3Properties properties, DefaultCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties properties, DefaultCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
