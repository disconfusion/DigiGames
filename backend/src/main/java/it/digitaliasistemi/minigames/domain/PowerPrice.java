package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Prezzo di un potere sovrascritto dall'admin (override del default del catalogo). */
@Entity
@Table(name = "power_price")
public class PowerPrice extends PanacheEntity {

    @Column(unique = true, nullable = false)
    public String powerId;

    @Column(nullable = false)
    public int cost;

    public static PowerPrice findByPowerId(String powerId) {
        return find("powerId", powerId).firstResult();
    }
}
