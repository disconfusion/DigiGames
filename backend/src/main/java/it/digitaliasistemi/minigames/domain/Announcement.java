package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Riga singola: testo della modale "Ultime Fix / Novità" modificabile dall'admin.
 * `revision` aumenta a ogni salvataggio: il frontend mostra la modale finché
 * l'utente non ha visto l'ultima revisione.
 */
@Entity
@Table(name = "announcement")
public class Announcement extends PanacheEntity {

    @Column(columnDefinition = "TEXT")
    public String content = "";

    @Column(nullable = false)
    public int revision = 0;

    public static Announcement getFirst() {
        return Announcement.<Announcement>findAll().firstResult();
    }
}
