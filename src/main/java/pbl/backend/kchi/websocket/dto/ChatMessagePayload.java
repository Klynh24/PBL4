package pbl.backend.kchi.websocket.dto;

public class ChatMessagePayload {

    private String senderId;
    private Long recipientId;
    private String text;
    private long timestamp;

    public ChatMessagePayload() {
    }

    public ChatMessagePayload(String senderId, Long recipientId, String text, long timestamp) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
