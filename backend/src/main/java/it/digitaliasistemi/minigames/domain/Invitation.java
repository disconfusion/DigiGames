package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/** Invito a giocare inviato da un utente a un altro. Notifica persistente (polling lato client). */
@Entity
@Table(name = "invitation")
public class Invitation extends PanacheEntity {

    @Column(nullable = false)
    public String fromUsername;

    @Column(nullable = false)
    public String fromDisplayName;

    @Column(nullable = false)
    public String toUsername;

    @Column(nullable = false)
    public String gameSlug;

    @Column(nullable = false)
    public String roomCode;

    /** PENDING | ACCEPTED | DECLINED */
    @Column(nullable = false)
    public String status = "PENDING";

    @Column(nullable = false)
    public Instant createdAt;

    public static List<Invitation> pendingFor(String username) {
        return list("toUsername = ?1 and status = ?2 order by createdAt desc", username, "PENDING");
    }

    public static long countPendingFor(String username) {
        return count("toUsername = ?1 and status = ?2", username, "PENDING");
    }
}
