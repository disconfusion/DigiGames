package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Casata (clan) scelta da un utente. Tabella separata da AppUser per evitare
 * l'ALTER ADD su app_user (inaffidabile in prod: vedi CLAUDE.md). Una sola
 * casata per utente.
 */
@Entity
@Table(name = "user_house", uniqueConstraints = @UniqueConstraint(columnNames = {"username"}))
public class UserHouse extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String username;

    @Column(nullable = false)
    public String house;

    public static UserHouse forUsername(String username) {
        return find("username", username).firstResult();
    }
}
