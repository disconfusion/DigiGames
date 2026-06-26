package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Quante cariche di un dato potere possiede un utente (inventario shop). */
@Entity
@Table(name = "owned_power",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "powerId"}))
public class OwnedPower extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String powerId;

    @Column(nullable = false)
    public int quantity = 0;

    public static OwnedPower find(String username, String powerId) {
        return find("username = ?1 and powerId = ?2", username, powerId).firstResult();
    }
}
