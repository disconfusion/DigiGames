package it.digitaliasistemi.minigames.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.Roadmap;
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
