package com.capteam.gaobackend.service.push;

import java.util.List;

public interface PushNotificationGateway {

    PushDispatchResult sendToTokens(List<String> tokens, PushMessageRequest request);
}
