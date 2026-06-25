package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Stato condiviso della parola del giorno — uno per data, visibile a tutti. */
@Entity
@Table(name = "daily_word_state")
public class DailyWordState extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public LocalDate date;

    /** Lettere corrette indovinate da qualsiasi utente, separate da virgola. */
    @Column(nullable = false, columnDefinition = "text")
    public String revealedLetters = "";

    /** Lettere sbagliate inserite da qualsiasi utente, separate da virgola. */
    @Column(nullable = false, columnDefinition = "text")
    public String wrongLetters = "";

    /** PLAYING, WON, LOST */
    @Column(nullable = false)
    public String status = "PLAYING";

    /** Username di chi ha indovinato la parola (null se ancora in gioco). */
    @Column
    public String winner;

    /** Parola impostata manualmente dall'admin (sovrascrive quella automatica). */
    @Column
    public String customWord;

    public static DailyWordState findByDate(LocalDate date) {
        return find("date", date).firstResult();
    }
}
