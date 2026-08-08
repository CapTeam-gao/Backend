package com.capteam.gaobackend.config;

import com.capteam.gaobackend.service.push.FirebasePushNotificationGateway;
import com.capteam.gaobackend.service.push.PushNotificationGateway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FirebaseProperties.class, JournalReminderProperties.class})
public class PushNotificationConfig {

    @Bean
    public PushNotificationGateway pushNotificationGateway(FirebaseProperties firebaseProperties) {
        return new FirebasePushNotificationGateway(firebaseProperties);
    }
}
