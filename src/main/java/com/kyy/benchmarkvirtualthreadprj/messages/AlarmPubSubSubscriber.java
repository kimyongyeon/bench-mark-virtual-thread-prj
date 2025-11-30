package com.kyy.benchmarkvirtualthreadprj.messages;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlarmPubSubSubscriber {

    private final AlarmSseService sseService;

    public void onMessage(String message, String channel) {

        String[] parts = channel.split(":");
        String userId = parts[2];

        sseService.sendToClient(userId, message, "alarm-update");
    }
}

