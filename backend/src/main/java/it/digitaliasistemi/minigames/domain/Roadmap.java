package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Riga singola: testo della roadmap modificabile dall'admin. */
@Entity
@Table(name = "roadmap")
public class Roadmap extends PanacheEntity {

    @Column(columnDefinition = "TEXT")
    public String content = "";

    public static Roadmap getFirst() {
        return Roadmap.<Roadmap>findAll().firstResult();
    }
}
