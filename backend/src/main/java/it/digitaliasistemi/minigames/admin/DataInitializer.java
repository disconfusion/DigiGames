package it.digitaliasistemi.minigames.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.Roadmap;
import it.digitaliasistemi.minigames.domain.Announcement;
import it.digitaliasistemi.minigames.domain.DailyRules;
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
        String roadmapContent = loadResource("roadmap.md");
        Roadmap r = Roadmap.getFirst();
        if (r == null) {
            r = new Roadmap();
            r.content = roadmapContent;
            r.persist();
            LOG.info("Roadmap creata da roadmap.md");
        } else if (!roadmapContent.equals(r.content)) {
            r.content = roadmapContent;
            LOG.info("Roadmap allineata a roadmap.md");
        }

        // Modale "Ultime Fix": announcement.md è la fonte di verità (stesso pattern della roadmap).
        // A ogni avvio il DB viene allineato al file; quando il testo cambia la revisione viene
        // incrementata, così la modale riappare agli utenti (oltre al reset per build-id a ogni
        // deploy). Basta editare announcement.md a ogni push per aggiornare le novità in prod.
        String announcementContent = loadResource("announcement.md");
        Announcement a = Announcement.getFirst();
        if (a == null) {
            a = new Announcement();
            a.content = announcementContent;
            a.revision = 1;
            a.persist();
            LOG.info("Announcement creata da announcement.md");
        } else if (!announcementContent.equals(a.content)) {
            a.content = announcementContent;
            a.revision += 1;
            LOG.infof("Announcement allineata a announcement.md (rev %d)", a.revision);
        }

        // Regole della Parola del Giorno: puro-DB, editabili dall'admin e permanenti.
        // A differenza di roadmap/announcement NON vengono riallineate a un file: si seeda
        // il default SOLO se la riga manca (prima installazione o dev drop-and-create),
        // così un edit da /admin non viene mai sovrascritto al riavvio.
        if (DailyRules.getFirst() == null) {
            DailyRules dr = new DailyRules();
            dr.content = DailyRules.DEFAULT;
            dr.persist();
            LOG.info("Regole Parola del Giorno seedate col default");
        }
    }

    /** Legge una risorsa di testo dal classpath (UTF-8); stringa vuota se assente o su errore. */
    private String loadResource(String name) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                LOG.warnf("%s non trovata nel classpath", name);
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warnf(e, "Errore lettura risorsa %s", name);
            return "";
        }
    }
}
