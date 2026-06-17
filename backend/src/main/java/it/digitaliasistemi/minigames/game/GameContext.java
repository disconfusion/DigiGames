package it.digitaliasistemi.minigames.game;

import it.digitaliasistemi.minigames.rooms.Room;

import java.util.Map;

/**
 * Contesto passato a un GameEngine per ogni messaggio: dà accesso alla stanza e ai
 * canali di invio (al mittente, a tutta la stanza, o a un singolo giocatore — utile
 * per i giochi con informazione nascosta come la battaglia navale).
 */
public interface GameContext {

    Room room();

    /** Email del giocatore che ha inviato il messaggio. */
    String senderEmail();

    /** Risponde solo al mittente. */
    void replyToSender(Map<String, Object> message);

    /** Invia a tutti i giocatori della stanza. */
    void broadcast(Map<String, Object> message);

    /** Invia solo alle connessioni di un determinato giocatore. */
    void sendTo(String email, Map<String, Object> message);
}
