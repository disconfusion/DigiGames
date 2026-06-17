package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "match_result")
public class MatchResult extends PanacheEntity {

    @Column(nullable = false)
    public String username;

    @Column(nullable = false)
    public String displayName;

    @Column(nullable = false)
    public String game;

    /** WIN, LOSE, DRAW */
    @Column(nullable = false)
    public String result;

    @Column(nullable = false)
    public Instant playedAt;
}
