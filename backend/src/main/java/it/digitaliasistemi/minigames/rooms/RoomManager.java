package it.digitaliasistemi.minigames.rooms;

import jakarta.enterprise.context.ApplicationScoped;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RoomManager {

    // Niente caratteri ambigui (0/O, 1/I).
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RNG = new SecureRandom();

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Room create(String gameSlug, String hostEmail, boolean isPrivate, int maxPlayers) {
        String code;
        do {
            code = randomCode();
        } while (rooms.containsKey(code));
        Room r = new Room(code, gameSlug, hostEmail, isPrivate, maxPlayers);
        r.players.add(hostEmail);
        rooms.put(code, r);
        return r;
    }

    public Room get(String code) {
        return code == null ? null : rooms.get(code.toUpperCase());
    }

    /** Stanze pubbliche ancora in attesa di giocatori (visibili in lobby). */
    public List<Room> listPublicWaiting() {
        return rooms.values().stream()
                .filter(r -> !r.isPrivate && r.status == Room.Status.WAITING)
                .toList();
    }

    public void remove(String code) {
        if (code != null) rooms.remove(code.toUpperCase());
    }

    public Collection<Room> all() {
        return rooms.values();
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
