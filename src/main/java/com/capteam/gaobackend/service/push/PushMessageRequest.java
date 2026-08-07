package com.capteam.gaobackend.service.push;

import java.util.Map;

public record PushMessageRequest(
        String title,
        String body,
        Map<String, String> data
) {
}
