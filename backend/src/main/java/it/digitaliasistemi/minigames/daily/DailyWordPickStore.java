package it.digitaliasistemi.minigames.daily;

import it.digitaliasistemi.minigames.domain.DailyWordPick;
import it.digitaliasistemi.minigames.words.WordService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;

/**
 * Accesso alla parola estratta per una giornata.
 *
 * <p>Bean a parte perché il salvataggio arriva dal thread che scarica la parola: passando da un
 * bean iniettato l'intercettore {@code @Transactional} viene applicato (una chiamata interna allo
 * stesso oggetto lo salterebbe).
 */
@ApplicationScoped
public class DailyWordPickStore {

    private static final Logger LOG = Logger.getLogger(DailyWordPickStore.class);

    public DailyWordPick find(LocalDate date) {
        return DailyWordPick.findByDate(date);
    }

    /** Salva la parola del giorno se non c'è già (due fetch in parallelo non creano doppioni). */
    @Transactional
    public void save(LocalDate date, WordService.Pick pick) {
        if (DailyWordPick.findByDate(date) != null) return;
        DailyWordPick p = new DailyWordPick();
        p.date = date;
        p.word = pick.word();
        p.lang = pick.lang();
        p.difficulty = pick.difficulty().key();
        p.fromDictionary = pick.fromApi();
        p.persist();
        LOG.infof("Parola del Giorno %s: difficoltà %s, %s", date, pick.difficulty().key(),
                pick.fromApi() ? "dal dizionario" : "dalle parole locali");
    }

    /** Rimuove la parola estratta per quella data (reset dell'admin). */
    @Transactional
    public void delete(LocalDate date) {
        DailyWordPick.delete("date", date);
    }
}
