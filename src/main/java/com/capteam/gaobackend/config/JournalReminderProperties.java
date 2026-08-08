package com.capteam.gaobackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "journal.reminder")
public record JournalReminderProperties(
        boolean enabled,
        String cron,
        String zone,
        // java.time.DayOfWeek 기준 값(MONDAY=1 ... SUNDAY=7). 캡스톤 일지 작성 요일(수요일)=3.
        int dayOfWeek,
        LocalTime deadlineTime,
        int minutesBefore,
        String title,
        String body
) {
}
