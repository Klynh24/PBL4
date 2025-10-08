package com.example.signaling.messaging;

import com.example.signaling.config.SignalingProperties;
import com.example.signaling.support.InstanceIdProvider;
import com.example.signaling.websocket.RoomRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "signaling.redis", name = "enabled", havingValue = "true")
public class RedisRoomMessageSubscriber implements MessageListener, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RedisRoomMessageSubscriber.class);

    private final RedisMessageListenerContainer container;
    private final SignalingProperties properties;
    private final ObjectMapper objectMapper;
    private final RoomRegistry roomRegistry;
    private final InstanceIdProvider instanceIdProvider;

    public RedisRoomMessageSubscriber(RedisMessageListenerContainer container,
                                      SignalingProperties properties,
                                      ObjectMapper objectMapper,
                                      RoomRegistry roomRegistry,
                                      InstanceIdProvider instanceIdProvider) {
        this.container = container;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.roomRegistry = roomRegistry;
        this.instanceIdProvider = instanceIdProvider;
    }

    @Override
    public void afterPropertiesSet() {
        String pattern = properties.getRedis().getChannelPrefix() + "*";
        container.addMessageListener(this, new PatternTopic(pattern));
        log.info("Subscribed to Redis signaling channels with pattern {}", pattern);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode node = objectMapper.readTree(message.getBody());
            String originInstanceId = node.path("originInstanceId").asText("");
            if (originInstanceId.equals(instanceIdProvider.getInstanceId())) {
                return;
            }
            String roomId = node.path("roomId").asText(null);
            String payload = node.path("payload").asText(null);
            if (roomId == null || payload == null) {
                log.debug("Skipping invalid Redis signaling message: {}", node);
                return;
            }
            String targetSubject = node.path("targetSubject").isTextual() ? node.path("targetSubject").asText() : null;
            roomRegistry.broadcastLocal(roomId, payload, null, targetSubject);
        } catch (Exception e) {
            log.warn("Failed to handle Redis signaling message: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        container.removeMessageListener(this);
    }
}
