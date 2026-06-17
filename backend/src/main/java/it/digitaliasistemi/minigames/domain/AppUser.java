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

    public static AppUser findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public static boolean usernameExists(String username) {
        return count("username", username) > 0;
    }
}
