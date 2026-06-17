package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class AppUser extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String displayName;

    @Column(nullable = false)
    public String role = "user";

    /** Avatar ASCII componibile: spec JSON compatta scelta dall'utente (es. {"eyes":2,"mouth":1,"acc":0}). Null = default. */
    @Column(columnDefinition = "text")
    public String avatar;

    public static AppUser findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static boolean usernameExists(String username) {
        return count("username", username) > 0;
    }
}
