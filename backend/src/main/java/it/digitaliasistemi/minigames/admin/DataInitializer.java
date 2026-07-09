package it.digitaliasistemi.minigames.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.Roadmap;
import it.digitaliasistemi.minigames.domain.Announcement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Crea l'account admin e la roadmap di default all'avvio se non esistono. */
@ApplicationScoped
public class DataInitializer {

    private static final Logger LOG = Logger.getLogger(DataInitializer.class);

    /**
     * Testo di default della modale "Ultime Fix" + revisione baseline. Vive nel codice: a ogni
     * deploy va aggiornato il testo e incrementata {@link #DEFAULT_ANNOUNCEMENT_REVISION}, così le
     * novità compaiono da sole (l'upsert in {@code onStart} allinea i DB con revisione inferiore).
     */
    private static final int DEFAULT_ANNOUNCEMENT_REVISION = 2;
    private static final String DEFAULT_ANNOUNCEMENT = """
        ## Novità e fix

        **Nuovo — Tris a sparizione**: modalità opzionale in cui ogni giocatore tiene al massimo 3 segni; piazzando il 4° sparisce il più vecchio. Niente più pareggi!

        **Impiccato**
        - La parola si può tentare solo nel proprio turno
        - Niente più stessa parola due volte di fila

        **Stanze multiplayer**
        - Lista giocatori sempre corretta entrando o ricaricando la pagina
        - Le stanze abbandonate ora si chiudono da sole

        **Companion** — orbite più varie attorno all'avatar

        Buon divertimento!
        """;

    @Transactional
    void onStart(@Observes StartupEvent ev) {
        if (!AppUser.usernameExists("admin")) {
            AppUser admin = new AppUser();
            admin.username = "admin";
            admin.displayName = "Admin";
            admin.passwordHash = BcryptUtil.bcryptHash("gianlucaGM");
            admin.role = "admin";
            admin.persist();
            LOG.info("Account admin creato (username: admin)");
        }

        // Roadmap repo-driven: roadmap.md è la fonte di verità.
        // A ogni avvio (incl. cold-start Render) il DB viene allineato al file,
        // così git e prod restano sempre sincronizzati.
        String fileContent = loadDefaultRoadmap();
        Roadmap r = Roadmap.getFirst();
        if (r == null) {
            r = new Roadmap();
            r.content = fileContent;
            r.persist();
            LOG.info("Roadmap creata da roadmap.md");
        } else if (!fileContent.equals(r.content)) {
            r.content = fileContent;
            LOG.info("Roadmap allineata a roadmap.md");
        }

        // Modale "Ultime Fix": il testo di default vive nel codice con una revisione baseline.
        // Viene applicato ai DB assenti o rimasti indietro (revision < baseline), così le novità di
        // ogni deploy compaiono da sole; se l'Admin l'ha aggiornata a una revisione superiore (da
        // /admin), quella vince e non viene sovrascritta.
        Announcement a = Announcement.getFirst();
        if (a == null) {
            a = new Announcement();
            a.content = DEFAULT_ANNOUNCEMENT;
            a.revision = DEFAULT_ANNOUNCEMENT_REVISION;
            a.persist();
            LOG.infof("Announcement (Ultime Fix) di default creato (rev %d)", DEFAULT_ANNOUNCEMENT_REVISION);
        } else if (a.revision < DEFAULT_ANNOUNCEMENT_REVISION) {
            a.content = DEFAULT_ANNOUNCEMENT;
            a.revision = DEFAULT_ANNOUNCEMENT_REVISION;
            LOG.infof("Announcement allineata al default di codice (rev %d)", DEFAULT_ANNOUNCEMENT_REVISION);
        }
    }

    private String loadDefaultRoadmap() {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("roadmap.md")) {
            if (in == null) {
                LOG.warn("roadmap.md non trovata nel classpath: roadmap di default vuota");
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("Errore lettura roadmap.md di default", e);
            return "";
        }
    }
}
