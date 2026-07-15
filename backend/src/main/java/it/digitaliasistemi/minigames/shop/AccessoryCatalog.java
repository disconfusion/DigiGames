package it.digitaliasistemi.minigames.shop;

import java.util.List;
import java.util.Optional;

/**
 * Catalogo statico degli accessori avatar (cosmetici) acquistabili con i Token.
 * L'id coincide con il nome della sprite pixel lato frontend (vedi
 * frontend/src/lib/icons/sprites.ts): così l'accessorio si rende con &lt;Icon name={id} /&gt;
 * come overlay sul volto ASCII. Ogni accessorio occupa uno {@code slot}
 * ("testa" | "occhi" | "bocca"): si può indossare un accessorio per slot in parallelo.
 */
public final class AccessoryCatalog {

    private AccessoryCatalog() {}

    /**
     * @param id          id stabile (coincide con la sprite frontend)
     * @param name        nome visibile
     * @param description testo descrittivo / lore
     * @param slot        slot indossabile: "testa" | "occhi" | "bocca"
     * @param defaultCost costo in Token
     */
    public record AccessoryDef(String id, String name, String description, String slot, int defaultCost) {}

    public static final List<AccessoryDef> ACCESSORIES = List.of(
        new AccessoryDef("acc_corona", "Corona",
            "Corona regale d'oro: incoronati sovrano della leaderboard.", "testa", 220),
        new AccessoryDef("acc_cilindro", "Cilindro",
            "Cappello a cilindro elegante, per un avatar di classe.", "testa", 150),
        new AccessoryDef("acc_cuffie", "Cuffie gaming",
            "Cuffie da gaming con neon: pronto alla sfida.", "testa", 150),
        new AccessoryDef("acc_occhiali", "Occhiali",
            "Occhiali da vista: sguardo da vero stratega.", "occhi", 120),
        new AccessoryDef("acc_shades", "Occhiali da sole",
            "Occhiali da sole: il cool non si spiega, si indossa.", "occhi", 140),
        new AccessoryDef("acc_baffi", "Baffi",
            "Un paio di baffi a manubrio, distinti e inconfondibili.", "bocca", 100)
    );

    public static Optional<AccessoryDef> byId(String id) {
        return ACCESSORIES.stream().filter(a -> a.id().equals(id)).findFirst();
    }
}
