package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.List;

/**
 * Accessorio avatar (cosmetico) posseduto da un utente. Tabella separata da AppUser
 * (come OwnedCompanion) per evitare l'ALTER ADD su app_user (inaffidabile in prod:
 * vedi CLAUDE.md). Multi-slot: un solo accessorio equipaggiato per slot (testa/occhi/bocca),
 * quindi più accessori possono essere indossati insieme (uno per slot).
 */
@Entity
@Table(name = "owned_accessory",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "accessoryId"}))
public class OwnedAccessory extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String accessoryId;

    /** Slot occupato (denormalizzato dal catalogo per l'equip per-slot). */
    @Column(nullable = false)
    public String slot;

    @Column(nullable = false)
    public boolean equipped = false;

    public static List<OwnedAccessory> forUser(String username) {
        return list("username", username);
    }

    public static OwnedAccessory find(String username, String accessoryId) {
        return find("username = ?1 and accessoryId = ?2", username, accessoryId).firstResult();
    }

    public static List<OwnedAccessory> findEquipped(String username) {
        return list("username = ?1 and equipped = ?2", username, true);
    }
}
