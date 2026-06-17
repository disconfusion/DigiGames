package it.digitaliasistemi.minigames.social;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Presenza online in memoria (singola istanza, niente Redis). I client fanno un "ping"
 * periodico via REST; è considerato online chi ha pingato negli ultimi {@link #TTL}.
 */
@ApplicationScoped
public class PresenceService {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public void touch(String username) {
        if (username != null) lastSeen.put(username, Instant.now());
    }

    public boolean isOnline(String username) {
        Instant t = lastSeen.get(username);
        return t != null && Duration.between(t, Instant.now()).compareTo(TTL) <= 0;
    }

    public Set<String> onlineUsers() {
        Instant now = Instant.now();
        return lastSeen.entrySet().stream()
                .filter(e -> Duration.between(e.getValue(), now).compareTo(TTL) <= 0)
                .map(java.util.Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
