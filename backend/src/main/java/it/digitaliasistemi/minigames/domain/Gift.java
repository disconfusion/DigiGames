package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

/**
 * Regalo pendente per un utente (Token, potere, companion o accessorio), in attesa di essere
 * mostrato nella modale al prossimo accesso. Il mittente può essere l'admin (from* null) o un
 * altro utente (dono user→user). Viene cancellato dopo la visione (ack) → la tabella resta
 * sempre piccola (limite storage Neon, vedi CLAUDE.md).
 */
@Entity
@Table(name = "gift")
public class Gift extends PanacheEntity {

    /** Destinatario (username minuscolo). */
    @Column(nullable = false)
    public String username;

    /** "tokens" | "power" | "companion" | "accessory". */
    @Column(nullable = false)
    public String type;

    /** Username del mittente per i doni user→user; null se assegnato dall'admin. */
    @Column
    public String fromUsername;

    /** Nome visualizzato del mittente (mostrato nella modale); null se admin. */
    @Column
    public String fromDisplayName;

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
