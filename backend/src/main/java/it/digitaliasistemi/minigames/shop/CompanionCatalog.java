package it.digitaliasistemi.minigames.shop;

import java.util.List;
import java.util.Optional;

/**
 * Catalogo statico dei companion (cosmetici) acquistabili con i Token.
 * L'id coincide con il nome della sprite pixel lato frontend (vedi
 * frontend/src/lib/icons/sprites.ts): cos&igrave; &lt;Icon name={id} /&gt; rende il companion.
 */
public final class CompanionCatalog {

    private CompanionCatalog() {}

    /**
     * @param id          id stabile (coincide con la sprite frontend)
     * @param name        nome visibile
     * @param description testo descrittivo / lore
     * @param defaultCost costo in Token
     */
    public record CompanionDef(String id, String name, String description, int defaultCost) {}

    public static final List<CompanionDef> COMPANIONS = List.of(
        new CompanionDef("gondola", "Gondola Maledetta",
            "Una gondola in fiamme che vaga senza pace nella laguna. Nel profilo orbita attorno a te.", 250),
        new CompanionDef("leone", "Leon de San Marco",
            "Il leone alato dorato, simbolo della Serenissima.", 150),
        new CompanionDef("mose", "MOSE",
            "Le paratoie che fermano l'acqua alta: si alzano e si abbassano accanto a te.", 150),
        new CompanionDef("dart180", "Centottanta!",
            "Il bersaglio delle freccette: il massimo punteggio in una mano.", 120),
        new CompanionDef("stambecco", "Stambecco",
            "Il re delle Alpi col suo corno ricurvo, dal Gran Paradiso.", 120),
        new CompanionDef("lupo", "Lupo",
            "Predatore di ritorno sull'Appennino e sulle Alpi.", 120),
        new CompanionDef("batman", "Pipistrello",
            "Il cavaliere oscuro veglia su Gotham (e sulla tua leaderboard).", 220),
        new CompanionDef("persona5", "Maschera Ladra",
            "La maschera bianca del ladro gentiluomo: ruba i cuori (e i Token).", 160),
        new CompanionDef("sly", "Maschera del Procione",
            "La mascherina blu del procione ladro acrobata.", 160)
    );

    public static Optional<CompanionDef> byId(String id) {
        return COMPANIONS.stream().filter(c -> c.id().equals(id)).findFirst();
    }
}
