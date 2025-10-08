package com.example.signaling.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "signaling.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpRoomMessagePublisher implements RoomMessagePublisher {

    @Override
    public void publish(String roomId,
                        String messagePayload,
                        String originSessionId,
                        String originInstanceId,
                        String targetSubject) {
        // no-op when Redis distribution is disabled
    }
}
