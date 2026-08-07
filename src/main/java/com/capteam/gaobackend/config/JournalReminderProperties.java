package com.capteam.gaobackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "journal.reminder")
public record JournalReminderProperties(
        boolean enabled,
        String cron,
        String zone,
        LocalTime deadlineTime,
        int minutesBefore,
        String title,
        String body
) {
}
