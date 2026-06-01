package com.capteam.gaobackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class ChatFileWebConfig implements WebMvcConfigurer {

    @Value("${chat.file.upload-dir}")
    private String uploadDir;

    @Value("${chat.file.public-path}")
    private String publicPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 업로드된 파일을 HTTP로 다시 내려받을 수 있게 정적 리소스 경로를 연결합니다.
        // 예) /chat-files/abc.png 요청 -> chat.file.upload-dir 폴더의 abc.png 파일
        String resourceLocation = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        String handlerPattern = publicPath.endsWith("/") ? publicPath + "**" : publicPath + "/**";

        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(resourceLocation);
    }
}
