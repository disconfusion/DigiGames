package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

/** Azioni per-utente per la parola del giorno. */
@Entity
@Table(name = "daily_attempt",
       uniqueConstraints = @UniqueConstraint(columnNames = {"date", "username"}))
public class DailyAttempt extends PanacheEntity {

    @Column(nullable = false)
    public LocalDate date;

    @Column(nullable = false)
    public String username;

    /** Ha già usato il suo slot lettera oggi. */
    @Column(nullable = false)
    public boolean letterUsed = false;

    /** Ha già usato il suo tentativo di indovinare la parola intera. */
    @Column(nullable = false)
    public boolean wordAttemptUsed = false;

    /** Ha indovinato la parola intera (vinto). */
    @Column(nullable = false)
    public boolean won = false;

    public static DailyAttempt findByDateAndUser(LocalDate date, String username) {
        return find("date = ?1 and username = ?2", date, username).firstResult();
    }
}
