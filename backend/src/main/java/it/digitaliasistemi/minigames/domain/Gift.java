package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/**
 * Regalo assegnato dall'admin a un utente (Token, potere o companion), in attesa di essere
 * mostrato nella modale al prossimo accesso. Viene cancellato dopo la visione (ack) → la
 * tabella resta sempre piccola (limite storage Neon, vedi CLAUDE.md).
 */
@Entity
@Table(name = "gift")
public class Gift extends PanacheEntity {

    /** Destinatario (username minuscolo). */
    @Column(nullable = false)
    public String username;

    /** "tokens" | "power" | "companion". */
    @Column(nullable = false)
    public String type;

    /** Sprite id del potere/companion; null per i Token. */
    public String itemId;

    /** Testo visibile ("Token", "Siluro di prossimità", "Gondola Maledetta"…). */
    @Column(nullable = false)
    public String label;

    /** Quantità: Token accreditati o cariche del potere; 1 per un companion. */
    @Column(nullable = false)
    public int amount = 1;

    @Column(nullable = false)
    public Instant createdAt = Instant.now();

    public static List<Gift> pendingFor(String username) {
        return list("username = ?1 order by createdAt", username);
    }
}
