package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Parola estratta per la Parola del Giorno di una data: viene scelta una volta e poi riusata,
 * altrimenti a ogni riavvio (cold start di Render compreso) la parola del giorno cambierebbe.
 *
 * <p>Tabella separata da {@code daily_word_state} di proposito: quella tiene lo stato di gioco
 * condiviso e in prod lo schema gira in {@code update}, dove l'aggiunta di colonne non è
 * affidabile mentre la creazione di nuove tabelle sì (vedi CLAUDE.md).
 */
@Entity
@Table(name = "daily_word_pick")
public class DailyWordPick extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public LocalDate date;

    @Column(nullable = false)
    public String word;

    /** Lingua richiesta al dizionario (la Parola del Giorno è sempre in italiano). */
    @Column(nullable = false, length = 8)
    public String lang = "it";

    /** Difficoltà estratta per la giornata: "facile" | "media" | "difficile". */
    @Column(nullable = false, length = 16)
    public String difficulty;

    /** false se il dizionario online non ha risposto e si è ripiegato sulle parole locali. */
    @Column(nullable = false)
    public boolean fromDictionary;

    public static DailyWordPick findByDate(LocalDate date) {
        return find("date", date).firstResult();
    }
}
