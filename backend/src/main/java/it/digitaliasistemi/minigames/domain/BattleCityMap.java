package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/**
 * Mappa di Battle City disegnata dai giocatori in Construction Mode e salvata per rigiocarla.
 * Le mappe sono visibili a tutti (l'idea è giocare quelle dei colleghi); può cancellarle
 * l'autore o un admin.
 *
 * <p>Le righe della griglia sono salvate in una sola colonna di testo separate da "\n": la
 * mappa è piccola e fissa (13×13) e così non serve una tabella figlia.
 */
@Entity
@Table(name = "battlecity_map")
public class BattleCityMap extends PanacheEntity {

    @Column(nullable = false, length = 60)
    public String name;

    /** Chi l'ha salvata. */
    @Column(nullable = false)
    public String authorUsername;

    @Column(nullable = false)
    public String authorDisplayName;

    /** Chi l'ha disegnata (username separati da virgola): in Construction Mode sono due. */
    @Column(columnDefinition = "text")
    public String builders;

    /** Le 13 righe della griglia, separate da newline. */
    @Column(nullable = false, columnDefinition = "text")
    public String rows;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    /** Quante volte è stata giocata: le mappe più usate finiscono in cima all'elenco. */
    @Column(nullable = false)
    public int plays = 0;

    public String[] rowArray() {
        return rows.split("\n");
    }

    public void setRowArray(String[] arr) {
        rows = String.join("\n", arr);
    }

    public static List<BattleCityMap> newest() {
        return list("order by createdAt desc");
    }
}
