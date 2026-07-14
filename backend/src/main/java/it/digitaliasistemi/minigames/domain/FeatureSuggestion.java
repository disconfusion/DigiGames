package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Suggerimento di feature o gioco inviato da un utente. Visibile solo all'admin.
 * Tabella separata da bug_report apposta per evitare l'ALTER ADD su schema prod (update):
 * i CREATE di tabelle nuove sono affidabili, gli ALTER ADD di colonne no.
 */
@Entity
@Table(name = "feature_suggestion")
public class FeatureSuggestion extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String displayName;

    /** "feature" oppure "game". */
    @Column(nullable = false)
    public String kind;

    @Column(nullable = false, columnDefinition = "text")
    public String description;

    @Column(nullable = false)
    public Instant createdAt;
}
