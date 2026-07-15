package it.digitaliasistemi.minigames.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Riga singola: testo delle regole della Parola del Giorno, modificabile dall'admin.
 * Puro-DB (a differenza di roadmap/announcement non è riallineato a un file): un default
 * viene seedato solo se la riga manca (DataInitializer), così gli edit dell'admin restano permanenti.
 */
@Entity
@Table(name = "daily_rules")
public class DailyRules extends PanacheEntity {

    @Column(columnDefinition = "TEXT")
    public String content = "";

    public static DailyRules getFirst() {
        return DailyRules.<DailyRules>findAll().firstResult();
    }

    /** Regole di default (Markdown), usate come seed iniziale se la riga non esiste. */
    public static final String DEFAULT = """
            - Ogni giorno **una sola parola**, uguale per tutti i colleghi.
            - Hai **una lettera** e **un tentativo di parola intera** per tutta la giornata.
            - Gli errori sono **condivisi**: a **6 errori** totali la parola è persa per tutti.
            - Indovini la parola → **vinci +10 punti**.
            - Sbagli il tentativo di parola → **eliminato** per oggi: puoi solo guardare.
            - Aggiornamento **in tempo reale** quando gli altri giocano.
            - Nuova parola a **mezzanotte (ora di Roma)**.
            """;
}
