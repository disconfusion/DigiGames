package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.List;

/**
 * Companion (cosmetico) posseduto da un utente. Tabella separata da AppUser così
 * il companion equipaggiato non richiede un ALTER ADD su app_user (inaffidabile
 * in prod: vedi CLAUDE.md). Un solo companion equipaggiato per utente.
 */
@Entity
@Table(name = "owned_companion",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "companionId"}))
public class OwnedCompanion extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String companionId;

    @Column(nullable = false)
    public boolean equipped = false;

    public static List<OwnedCompanion> forUser(String username) {
        return list("username", username);
    }

    public static OwnedCompanion find(String username, String companionId) {
        return find("username = ?1 and companionId = ?2", username, companionId).firstResult();
    }

    public static OwnedCompanion findEquipped(String username) {
        // NB: 3 argomenti (query + 2 param) per non collidere col metodo custom find(String, String).
        return find("username = ?1 and equipped = ?2", username, true).firstResult();
    }
}
