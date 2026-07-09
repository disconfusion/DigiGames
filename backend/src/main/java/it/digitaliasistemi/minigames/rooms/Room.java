package it.digitaliasistemi.minigames.rooms;

import com.fasterxml.jackson.databind.JsonNode;

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
    /** Opzioni di gioco scelte alla creazione (interpretate dall'engine, es. config impiccato). Può essere null. */
    public final JsonNode options;
    public volatile Status status = Status.WAITING;
    public final Set<String> players = ConcurrentHashMap.newKeySet();

    /** Stato della partita in corso (tipo dipende da gameSlug, es. HangmanState). Null se non avviata. */
    public volatile Object game;

    /**
     * Epoch-ms del primo istante in cui la stanza è stata vista SENZA alcun socket connesso.
     * 0 = ci sono connessioni (o non ancora valutata). Usato dal {@code RoomReaper} per rimuovere
     * le stanze fantasma dopo un periodo di grazia (tollera i reconnect transienti).
     */
    public volatile long emptySince = 0;

    public Room(String code, String gameSlug, String hostEmail, boolean isPrivate, int maxPlayers, JsonNode options) {
        this.code = code;
        this.gameSlug = gameSlug;
        this.hostEmail = hostEmail;
        this.isPrivate = isPrivate;
        this.maxPlayers = maxPlayers;
        this.options = options;
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
}
