package it.digitaliasistemi.minigames.game.hangman;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Regole personalizzabili dell'impiccato, scelte dall'host alla creazione stanza.
 *
 * @param maxVowels        vocali totali chiamabili nell'intera partita (condiviso tra tutti);
 *                         0 = illimitate. Classica = 2.
 * @param lettersPerPlayer tentativi-lettera totali per ogni giocatore; 0 = illimitati.
 * @param accessories      accessori dell'impiccato (es. "hat", "pipe", "shoes"): ognuno aggiunge
 *                         +1 errore consentito e viene disegnato in figura.
 */
public record HangmanConfig(int maxVowels, int lettersPerPlayer, List<String> accessories) {

    /** Parti del corpo base (testa, corpo, 2 braccia, 2 gambe). */
    public static final int BASE_PARTS = 6;

    /** Accessori riconosciuti, in ordine di disegno. */
    public static final List<String> KNOWN_ACCESSORIES = List.of("hat", "pipe", "shoes");

    public HangmanConfig {
        accessories = accessories == null ? List.of() : List.copyOf(accessories);
    }

    /** Errori consentiti = parti del corpo base + un accessorio ciascuno. */
    public int maxWrong() {
        return BASE_PARTS + accessories.size();
    }

    /** Preset "Classica": nessun accessorio, 6 parti, 2 vocali max, lettere illimitate. */
    public static HangmanConfig classic() {
        return new HangmanConfig(2, 0, List.of());
    }

    /** Costruisce la config dalle opzioni JSON della stanza; valori mancanti = default classico. */
    public static HangmanConfig fromJson(JsonNode o) {
        if (o == null || o.isNull() || o.isMissingNode()) {
            return classic();
        }
        int maxVowels = Math.max(0, o.path("maxVowels").asInt(2));
        int lettersPerPlayer = Math.max(0, o.path("lettersPerPlayer").asInt(0));
        List<String> acc = new ArrayList<>();
        JsonNode arr = o.path("accessories");
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                String key = n.asText("");
                if (KNOWN_ACCESSORIES.contains(key) && !acc.contains(key)) {
                    acc.add(key);
                }
            }
        }
        // Mantieni l'ordine canonico di disegno
        acc.sort((a, b) -> Integer.compare(KNOWN_ACCESSORIES.indexOf(a), KNOWN_ACCESSORIES.indexOf(b)));
        return new HangmanConfig(maxVowels, lettersPerPlayer, acc);
    }
}
