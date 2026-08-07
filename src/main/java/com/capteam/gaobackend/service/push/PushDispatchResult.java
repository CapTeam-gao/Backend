package com.capteam.gaobackend.service.push;

import java.util.List;

public record PushDispatchResult(
        int successCount,
        int failureCount,
        List<String> failureReasons
) {

    public static PushDispatchResult failure(int targetCount, String reason) {
        return new PushDispatchResult(0, targetCount, List.of(reason));
    }
}
