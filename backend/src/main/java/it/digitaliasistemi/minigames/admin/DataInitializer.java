package it.digitaliasistemi.minigames.admin;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import it.digitaliasistemi.minigames.domain.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

/** Crea l'account admin all'avvio se non esiste. */
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
    }
}
