package pbl.backend.kchi.websocket.dto;

import java.util.List;

public class RoomEventPayload {

    public enum Type {
        JOIN,
        LEAVE,
        CHAT,
        USER_LIST
    }

    private Type type;
    private Long roomId;
    private String userId;
    private String text;
    private Long timestamp;
    private List<String> userIds;

    public RoomEventPayload() {
    }

    public RoomEventPayload(Type type, Long roomId, String userId, String text, Long timestamp, List<String> userIds) {
        this.type = type;
        this.roomId = roomId;
        this.userId = userId;
        this.text = text;
        this.timestamp = timestamp;
        this.userIds = userIds;
    }

    public static RoomEventPayload join(Long roomId, String userId) {
        return new RoomEventPayload(Type.JOIN, roomId, userId, null, System.currentTimeMillis(), null);
    }

    public static RoomEventPayload leave(Long roomId, String userId) {
        return new RoomEventPayload(Type.LEAVE, roomId, userId, null, System.currentTimeMillis(), null);
    }

    public static RoomEventPayload chat(Long roomId, String userId, String text) {
        return new RoomEventPayload(Type.CHAT, roomId, userId, text, System.currentTimeMillis(), null);
    }

    public static RoomEventPayload userList(Long roomId, List<String> userIds) {
        return new RoomEventPayload(Type.USER_LIST, roomId, null, null, System.currentTimeMillis(), userIds);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}
