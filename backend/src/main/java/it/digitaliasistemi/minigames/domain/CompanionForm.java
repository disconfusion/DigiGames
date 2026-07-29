package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Forma alternativa scelta da un utente per un companion che ne prevede più di una
 * (es. il pipistrello, trasformabile in vampiro dall'Area personale).
 *
 * <p>Tabella a sé, come {@code companion_tint}: in prod lo schema gira in {@code update}, che crea
 * le tabelle mancanti in modo affidabile ma non aggiunge colonne (vedi CLAUDE.md).
 */
@Entity
@Table(name = "companion_form",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "companionId"}))
public class CompanionForm extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String companionId;

    /** Nome della forma; "base" equivale a nessuna trasformazione. */
    @Column(nullable = false, length = 32)
    public String form;

    public static CompanionForm find(String username, String companionId) {
        return find("username = ?1 and companionId = ?2", username, companionId).firstResult();
    }
}
