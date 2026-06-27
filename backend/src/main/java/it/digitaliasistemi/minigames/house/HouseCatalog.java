package it.digitaliasistemi.minigames.house;

import java.util.List;

/**
 * Catalogo statico delle casate (clan). Per l'alpha le 4 case in stile Hogwarts.
 * L'id coincide con il nome della sprite stemma lato frontend (sprites.ts).
 */
public final class HouseCatalog {

    private HouseCatalog() {}

    public record HouseDef(String id, String name) {}

    public static final List<HouseDef> HOUSES = List.of(
        new HouseDef("grifondoro", "Grifondoro"),
        new HouseDef("serpeverde", "Serpeverde"),
        new HouseDef("corvonero", "Corvonero"),
        new HouseDef("tassorosso", "Tassorosso")
    );

    public static boolean isValid(String id) {
        return HOUSES.stream().anyMatch(h -> h.id().equals(id));
    }
}
