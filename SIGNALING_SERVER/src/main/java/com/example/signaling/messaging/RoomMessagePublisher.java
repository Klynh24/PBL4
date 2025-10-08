package com.example.signaling.messaging;

public interface RoomMessagePublisher {

    void publish(String roomId,
                 String messagePayload,
                 String originSessionId,
                 String originInstanceId,
                 String targetSubject);
}
