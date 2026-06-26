package it.digitaliasistemi.minigames.shop;

import java.util.List;
import java.util.Optional;

/**
 * Catalogo statico dei poteri acquistabili nello shop con i Token.
 * I costi qui sono i DEFAULT: l'admin può sovrascriverli (vedi PowerPrice / ShopService).
 *
 * <p>Ogni potere è legato a un gioco ({@code game}) e ha un'indicazione d'uso ({@code usage})
 * mostrata nell'HUD del cyberdeck lato frontend.
 */
public final class PowerCatalog {

    private PowerCatalog() {}

    /**
     * @param id          identificatore stabile (chiave inventario/prezzi)
     * @param game        slug del gioco a cui si applica
     * @param label       nome visibile
     * @param emoji       icona
     * @param description cosa fa
     * @param usage       istruzioni d'uso per l'HUD ("come si usa")
     * @param phase       quando è usabile: "PLACEMENT" | "BATTLE" | "ANY"
     * @param defaultCost costo di default in Token
     */
    public record PowerDef(
        String id,
        String game,
        String label,
        String emoji,
        String description,
        String usage,
        String phase,
        int defaultCost
    ) {}

    /** Poteri della Battaglia Navale (primo gioco con poteri). */
    public static final List<PowerDef> POWERS = List.of(
        new PowerDef("bs_torpedo", "battleship", "Siluro di prossimità", "🎯",
            "Spari e il colpo rivela quanto è vicina la nave più vicina: verde a 1 cella, giallo a 2, rosso a 3+.",
            "Seleziona una cella della griglia nemica: oltre all'esito vedrai un alone colorato di prossimità.",
            "BATTLE", 20),
        new PowerDef("bs_radar", "battleship", "Radar", "📡",
            "Scegli una cella nemica: rivela il contenuto delle 8 caselle adiacenti (nave o acqua), senza sparare.",
            "Seleziona una cella della griglia nemica: per qualche istante vedrai cosa c'è intorno.",
            "BATTLE", 30),
        new PowerDef("bs_move_ship", "battleship", "Sposta barca", "↔️",
            "Sposta una tua nave in una nuova posizione (se libera). Utile per sfuggire ai colpi.",
            "Seleziona una tua nave, poi la nuova posizione e orientamento.",
            "ANY", 40),
        new PowerDef("bs_extend_ship", "battleship", "Espandi barca", "➕",
            "Allunga una tua nave di una cella (se c'è spazio). Più bersaglio ma più resistenza.",
            "Seleziona una tua nave da allungare.",
            "ANY", 60),
        new PowerDef("bs_expand_board", "battleship", "Espandi tabellone", "🗺️",
            "Aggiunge 3 righe al tuo tabellone: più acqua, più difficile per l'avversario colpirti.",
            "Attivalo: il tuo tabellone si espande di 3 righe in basso.",
            "ANY", 80),
        new PowerDef("bs_extra_ship", "battleship", "Nave extra", "🚢",
            "Aggiunge una nave in più alla tua flotta, piazzata in uno spazio libero.",
            "Attivalo: una nuova nave comparirà sul tuo tabellone.",
            "ANY", 100),
        new PowerDef("bs_decoy", "battleship", "Esca", "🪤",
            "Marca una tua cella: se l'avversario la colpisce, il colpo viene deviato su una tua nave a caso.",
            "Seleziona una cella di una tua nave da proteggere con l'esca.",
            "ANY", 120)
    );

    public static Optional<PowerDef> byId(String id) {
        return POWERS.stream().filter(p -> p.id().equals(id)).findFirst();
    }

    public static List<PowerDef> forGame(String game) {
        return POWERS.stream().filter(p -> p.game().equals(game)).toList();
    }
}
