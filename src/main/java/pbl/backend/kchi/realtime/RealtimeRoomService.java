package pbl.backend.kchi.realtime;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class RealtimeRoomService {

    public record LeaveResult(Long roomId, String userId) {
    }

    private final Map<Long, Set<String>> roomToActiveUsers = new ConcurrentHashMap<>();
    private final Map<String, SessionInfo> sessionToInfo = new ConcurrentHashMap<>();

    private record SessionInfo(Long roomId, String userId) {
    }

    public void joinRoom(Long roomId, String userId, String sessionId) {
        if (roomId == null || userId == null || sessionId == null) {
            throw new IllegalArgumentException("roomId, userId, sessionId must be provided");
        }

        roomToActiveUsers
                .computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet())
                .add(userId);

        sessionToInfo.put(sessionId, new SessionInfo(roomId, userId));
    }

    public LeaveResult leaveBySession(String sessionId) {
        if (sessionId == null)
            return null;

        SessionInfo info = sessionToInfo.remove(sessionId);
        if (info == null)
            return null;

        Set<String> users = roomToActiveUsers.get(info.roomId());
        if (users != null) {
            users.remove(info.userId());
            if (users.isEmpty()) {
                roomToActiveUsers.remove(info.roomId());
            }
        }

        return new LeaveResult(info.roomId(), info.userId());
    }

    public Set<String> getActiveUsers(Long roomId) {
        Set<String> users = roomToActiveUsers.get(roomId);
        return users == null ? Collections.emptySet() : Collections.unmodifiableSet(users);
    }
}
