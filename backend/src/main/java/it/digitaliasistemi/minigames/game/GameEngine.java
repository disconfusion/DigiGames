package it.digitaliasistemi.minigames.game;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Un gioco. Ogni implementazione è un bean CDI (@ApplicationScoped) auto-scoperto
 * dal registry {@link GameEngines}. Lo stato della partita vive in {@code room.game}.
 * Il server è l'autorità: valida ogni azione qui.
 */
public interface GameEngine {

    /** Slug del gioco (deve combaciare con Room.gameSlug), es. "connect4". */
    String slug();

    /** Numero massimo di giocatori in stanza per questo gioco. */
    int maxPlayers();

    /** Gestisce un messaggio di gioco (es. "game:start", "move", "guess", "answer"). */
    void onMessage(GameContext ctx, String type, JsonNode payload);

    /** Chiamato quando un giocatore entra: l'engine può allinearlo allo stato corrente. */
    default void onJoin(GameContext ctx) {}

    /** Chiamato quando un giocatore lascia esplicitamente la stanza: l'engine può aggiornare lo stato. */
    default void onLeave(GameContext ctx) {}
}
