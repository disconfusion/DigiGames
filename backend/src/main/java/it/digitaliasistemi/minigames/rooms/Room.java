package it.digitaliasistemi.minigames.rooms;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stanza di gioco tenuta in memoria (Fly.io = singola istanza, nessun Redis).
 * Lo stato della partita vero e proprio verra' agganciato qui negli step successivi.
 */
public class Room {

    public enum Status { WAITING, PLAYING, DONE }

    public final String code;
    public final String gameSlug;
    public final String hostEmail;
    public final boolean isPrivate;
    public final int maxPlayers;
    public volatile Status status = Status.WAITING;
    public final Set<String> players = ConcurrentHashMap.newKeySet();

    /** Stato della partita in corso (tipo dipende da gameSlug, es. HangmanState). Null se non avviata. */
    public volatile Object game;

    public Room(String code, String gameSlug, String hostEmail, boolean isPrivate, int maxPlayers) {
        this.code = code;
        this.gameSlug = gameSlug;
        this.hostEmail = hostEmail;
        this.isPrivate = isPrivate;
        this.maxPlayers = maxPlayers;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
}
