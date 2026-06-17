package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/** Segnalazione bug inviata da un utente. Visibile solo all'admin. */
@Entity
@Table(name = "bug_report")
public class BugReport extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String displayName;

    /** Slug del gioco interessato, oppure "generale". */
    @Column(nullable = false)
    public String game;

    @Column(nullable = false, columnDefinition = "text")
    public String description;

    @Column(nullable = false)
    public Instant createdAt;
}
