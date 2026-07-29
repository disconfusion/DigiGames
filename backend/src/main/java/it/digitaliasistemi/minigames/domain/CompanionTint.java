package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Colore scelto da un utente per un companion ricolorabile (es. lo scarabeo rinoceronte).
 *
 * <p>Tabella separata da {@code owned_companion} per la stessa ragione per cui quella è separata
 * da {@code app_user}: in prod lo schema gira in modalit&agrave; {@code update}, che crea le
 * tabelle mancanti in modo affidabile ma non aggiunge colonne (vedi CLAUDE.md).
 */
@Entity
@Table(name = "companion_tint",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "companionId"}))
public class CompanionTint extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String companionId;

    /** Colore in formato #rrggbb. */
    @Column(nullable = false, length = 7)
    public String hex;

    public static CompanionTint find(String username, String companionId) {
        return find("username = ?1 and companionId = ?2", username, companionId).firstResult();
    }
}
