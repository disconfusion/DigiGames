package it.digitaliasistemi.minigames.game.battlecity;

import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.BattleCityMap;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Accesso al DB per le mappe di Battle City.
 *
 * <p>Bean a parte perché l'engine lo chiama dal thread del WebSocket: passando da un bean
 * iniettato l'intercettore {@code @Transactional} viene applicato (una chiamata interna allo
 * stesso oggetto lo salterebbe e {@code persist()} finirebbe fuori transazione).
 */
@ApplicationScoped
public class BattleCityMapStore {

    private static final Logger LOG = Logger.getLogger(BattleCityMapStore.class);

    /** Salva la mappa disegnata in stanza. */
    @Transactional
    public void save(String username, String name, String[] rows, List<String> builders) {
        AppUser u = AppUser.findByUsername(username);
        BattleCityMap m = new BattleCityMap();
        m.name = name.substring(0, Math.min(60, name.length()));
        m.authorUsername = username;
        m.authorDisplayName = u != null ? u.displayName : username;
        m.builders = builders == null || builders.isEmpty() ? username : String.join(",", builders);
        m.setRowArray(rows);
        m.persist();
        LOG.infof("Battle City: mappa \"%s\" salvata da %s", m.name, username);
    }

    /** Legge una mappa della libreria e ne conta la giocata; null se non esiste. */
    @Transactional
    public String[] readAndCountPlay(long mapId) {
        BattleCityMap m = BattleCityMap.findById(mapId);
        if (m == null) return null;
        m.plays++;
        return m.rowArray();
    }
}
