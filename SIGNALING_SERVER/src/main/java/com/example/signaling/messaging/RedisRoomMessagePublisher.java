package com.example.signaling.messaging;

import com.example.signaling.config.SignalingProperties;
import com.example.signaling.support.InstanceIdProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(prefix = "signaling.redis", name = "enabled", havingValue = "true")
public class RedisRoomMessagePublisher implements RoomMessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisRoomMessagePublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final SignalingProperties properties;
    private final ObjectMapper objectMapper;
    private final InstanceIdProvider instanceIdProvider;

    public RedisRoomMessagePublisher(StringRedisTemplate redisTemplate,
                                     SignalingProperties properties,
                                     ObjectMapper objectMapper,
                                     InstanceIdProvider instanceIdProvider) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.instanceIdProvider = instanceIdProvider;
    }

    @Override
    public void publish(String roomId,
                        String messagePayload,
                        String originSessionId,
                        String originInstanceId,
                        String targetSubject) {
        if (originInstanceId == null) {
            originInstanceId = instanceIdProvider.getInstanceId();
        }
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("roomId", roomId);
        envelope.put("payload", messagePayload);
        envelope.put("originSessionId", originSessionId);
        envelope.put("originInstanceId", originInstanceId);
        if (targetSubject != null) {
            envelope.put("targetSubject", targetSubject);
        }
        try {
            redisTemplate.convertAndSend(channelName(roomId), objectMapper.writeValueAsString(envelope));
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish signaling payload to Redis: {}", e.getMessage());
        }
    }

    private String channelName(String roomId) {
        return properties.getRedis().getChannelPrefix() + roomId;
    }
}
