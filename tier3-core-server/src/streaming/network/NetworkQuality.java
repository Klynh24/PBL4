package streaming.network;

/**
 * Network quality classification based on packet loss rate
 */
public enum NetworkQuality {
    UNKNOWN,      // No data yet
    GOOD,         // < 5% packet loss
    POOR,         // 5-15% packet loss
    CRITICAL      // > 15% packet loss
}

