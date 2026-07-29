package it.digitaliasistemi.minigames.game;

import java.util.Map;

/**
 * Canale di invio verso una stanza <b>indipendente dalla connessione</b> del mittente.
 *
 * <p>{@link GameContext} basta ai giochi a turni, dove ogni invio nasce da un messaggio ricevuto.
 * I giochi realtime (Pong) trasmettono invece dal proprio loop di gioco, su un thread che non ha
 * né connessione né contesto di richiesta: per loro serve questo canale, risolto per codice stanza.
 */
public interface RoomChannel {

    /** Invia a tutte le connessioni della stanza. */
    void broadcast(String roomCode, Map<String, Object> message);

    /** Invia alle sole connessioni di un giocatore dentro la stanza. */
    void sendTo(String roomCode, String username, Map<String, Object> message);

    /** C'è almeno una connessione aperta sulla stanza? I giochi realtime pausano se no. */
    boolean hasListeners(String roomCode);
}
