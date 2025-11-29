package streaming.quality;

import streaming.network.NetworkQuality;
import streaming.network.NetworkQualityMonitor;
import model.ClientStreamState;
import server.ClientHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ✅ CRITICAL FIX: Per-Client Adaptive Quality
 * 
 * Problem: "Worst Client Wins" - all clients get downgraded when one has bad
 * network
 * Solution: Adaptive quality per client based on their network conditions
 * 
 * Benefits:
 * - Good clients get high quality
 * - Bad clients get lower quality
 * - Fair resource distribution
 * - Better overall user experience
 */
public class PerClientQualityManager {
    private final NetworkQualityMonitor networkMonitor;
    private final ConcurrentHashMap<String, ClientHandler> clientHandlers;
    private final Map<String, QualityLevel> clientQualities = new ConcurrentHashMap<>();

    // Quality presets - using enum values directly
    private static final QualityLevel QUALITY_HIGH = QualityLevel.HIGH;
    private static final QualityLevel QUALITY_MEDIUM = QualityLevel.MEDIUM;
    private static final QualityLevel QUALITY_LOW = QualityLevel.LOW;
    private static final QualityLevel QUALITY_VERY_LOW = QualityLevel.MINIMAL;

    // Evaluation interval
    private static final long EVALUATION_INTERVAL_MS = 5000; // Every 5 seconds
    private long lastEvaluationTime = 0;

    public PerClientQualityManager(NetworkQualityMonitor networkMonitor,
            ConcurrentHashMap<String, ClientHandler> clientHandlers) {
        this.networkMonitor = networkMonitor;
        this.clientHandlers = clientHandlers;
    }

    /**
     * Evaluate and adjust quality for all clients
     * Call this periodically (e.g., every 5 seconds)
     */
    public void evaluateAllClients() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastEvaluationTime < EVALUATION_INTERVAL_MS) {
            return; // Too soon
        }

        lastEvaluationTime = currentTime;

        for (String clientId : clientHandlers.keySet()) {
            evaluateClient(clientId);
        }
    }

    /**
     * Evaluate and adjust quality for a single client
     */
    public void evaluateClient(String clientId) {
        ClientHandler handler = clientHandlers.get(clientId);
        if (handler == null) {
            return;
        }

        // Get current network quality
        NetworkQuality networkQuality = networkMonitor.assessQuality(clientId);

        // Determine appropriate quality level
        QualityLevel newQuality = determineQualityLevel(networkQuality, clientId);

        // Check if quality changed
        QualityLevel currentQuality = clientQualities.get(clientId);

        if (currentQuality == null || !currentQuality.equals(newQuality)) {
            // Quality changed, notify client
            clientQualities.put(clientId, newQuality);
            sendQualityChange(handler, newQuality);

            System.out.println("[PerClientQuality] Client " + clientId +
                    " quality changed: " + (currentQuality != null ? currentQuality.name() : "NONE") +
                    " -> " + newQuality.name() + " (Network: " + networkQuality + ")");
        }
    }

    /**
     * Determine quality level based on network conditions
     */
    private QualityLevel determineQualityLevel(NetworkQuality networkQuality, String clientId) {
        ClientHandler handler = clientHandlers.get(clientId);
        if (handler == null) {
            return QUALITY_LOW;
        }

        ClientStreamState streamState = handler.getStreamState();

        switch (networkQuality) {
            case GOOD:
                // Good network - high quality
                // But check packet loss rate
                if (streamState != null && streamState.getPacketLossRate() < 0.02) {
                    return QUALITY_HIGH;
                } else {
                    return QUALITY_MEDIUM;
                }

            case POOR:
                // Poor network - medium or low quality
                if (streamState != null && streamState.getPacketLossRate() < 0.05) {
                    return QUALITY_MEDIUM;
                } else {
                    return QUALITY_LOW;
                }

            case CRITICAL:
                // Critical network - very low quality
                return QUALITY_VERY_LOW;

            case UNKNOWN:
            default:
                // No data yet, start with medium
                return QUALITY_MEDIUM;
        }
    }

    /**
     * Send quality change notification to client
     */
    private void sendQualityChange(ClientHandler handler, QualityLevel quality) {
        String message = String.format("SET_QUALITY:%s:%d:%d:%d:%.2f",
                quality.name(),
                quality.width,
                quality.height,
                quality.fps,
                quality.jpegQuality);

        handler.sendMessage(message);
    }

    /**
     * Get current quality for client
     */
    public QualityLevel getClientQuality(String clientId) {
        return clientQualities.getOrDefault(clientId, QUALITY_MEDIUM);
    }

    /**
     * Remove client
     */
    public void removeClient(String clientId) {
        clientQualities.remove(clientId);
    }

    /**
     * Print statistics
     */
    public void printStats() {
        System.out.println("\n=== Per-Client Quality Statistics ===");

        Map<String, Integer> qualityCounts = new ConcurrentHashMap<>();

        for (QualityLevel quality : clientQualities.values()) {
            qualityCounts.merge(quality.name(), 1, Integer::sum);
        }

        System.out.println("Total Clients: " + clientQualities.size());
        for (Map.Entry<String, Integer> entry : qualityCounts.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " clients");
        }

        System.out.println("=====================================\n");
    }
}
