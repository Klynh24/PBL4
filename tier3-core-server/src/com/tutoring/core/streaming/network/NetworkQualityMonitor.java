package com.tutoring.core.streaming.network;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkQualityMonitor {
    private static final int NACK_WINDOW_SECONDS = 10; // 10-second sliding window
    private static final double POOR_THRESHOLD = 0.05; // 5% packet loss = POOR
    private static final double CRITICAL_THRESHOLD = 0.15; // 15% packet loss = CRITICAL

    private final Map<String, ClientNetworkStats> clientStats = new ConcurrentHashMap<>();

    public void recordPacketSent(String clientId) {
        ClientNetworkStats stats = getOrCreateStats(clientId);
        stats.incrementPacketsSent();
    }

    public void recordNACK(String clientId, int missedPacketCount) {
        ClientNetworkStats stats = getOrCreateStats(clientId);
        stats.recordNACKs(missedPacketCount);

        if (missedPacketCount > 10) {
            System.out.println("[NetworkMonitor] High NACK from client " + clientId +
                    ": " + missedPacketCount + " packets");
        }
    }

    public NetworkQuality assessQuality(String clientId) {
        ClientNetworkStats stats = clientStats.get(clientId);
        if (stats == null)
            return NetworkQuality.UNKNOWN;

        double lossRate = stats.calculatePacketLossRate();

        if (lossRate >= CRITICAL_THRESHOLD) {
            return NetworkQuality.CRITICAL; // > 15% loss
        } else if (lossRate >= POOR_THRESHOLD) {
            return NetworkQuality.POOR; // 5-15% loss
        } else {
            return NetworkQuality.GOOD; // < 5% loss
        }
    }

    public ClientNetworkStats getStats(String clientId) {
        return clientStats.get(clientId);
    }

    public Set<String> getAllClients() {
        return clientStats.keySet();
    }

    public void removeClient(String clientId) {
        ClientNetworkStats stats = clientStats.remove(clientId);
        if (stats != null) {
            stats.clear();
        }
    }

    private ClientNetworkStats getOrCreateStats(String clientId) {
        return clientStats.computeIfAbsent(
                clientId,
                k -> new ClientNetworkStats(NACK_WINDOW_SECONDS));
    }

    public void printStats() {
        System.out.println("\n=== Network Quality Statistics ===");
        System.out.println("Clients monitored: " + clientStats.size());

        for (Map.Entry<String, ClientNetworkStats> entry : clientStats.entrySet()) {
            String clientId = entry.getKey();
            ClientNetworkStats stats = entry.getValue();
            NetworkQuality quality = assessQuality(clientId);

            double lossRate = stats.calculatePacketLossRate() * 100;

            System.out.printf("  Client %s: %s (Loss: %.2f%%, Sent: %d, Lost: %d, Samples: %d)%n",
                    clientId.substring(0, Math.min(8, clientId.length())),
                    quality,
                    lossRate,
                    stats.getTotalPacketsSent(),
                    stats.getTotalPacketsLost(),
                    stats.getSampleCount());
        }
        System.out.println("==================================\n");
    }

    public void clearAll() {
        clientStats.values().forEach(ClientNetworkStats::clear);
        clientStats.clear();
    }
}
