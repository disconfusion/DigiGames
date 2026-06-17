package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Utente registrato. Solo email dei domini aziendali consentiti (vedi AuthService).
 * Password salvata come hash bcrypt (mai in chiaro).
 */
@Entity
@Table(name = "app_user")
public class AppUser extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String email;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String displayName;

    @Column(nullable = false)
    public String role = "user";

    public static AppUser findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public static boolean emailExists(String email) {
        return count("email", email) > 0;
    }
}
