package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "daily_attempt",
       uniqueConstraints = @UniqueConstraint(columnNames = {"date", "username"}))
public class DailyAttempt extends PanacheEntity {

    @Column(nullable = false)
    public LocalDate date;

    @Column(nullable = false)
    public String username;

    /** Lettere corrette indovinate, separate da virgola. */
    @Column(nullable = false, columnDefinition = "text")
    public String guessed = "";

    /** Lettere errate, separate da virgola. */
    @Column(nullable = false, columnDefinition = "text")
    public String wrong = "";

    @Column(nullable = false)
    public String status = "PLAYING";

    public static DailyAttempt findByDateAndUser(LocalDate date, String username) {
        return find("date = ?1 and username = ?2", date, username).firstResult();
    }
}
