package it.digitaliasistemi.minigames.web;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Due responsabilità:
 * 1. Cache-Control per le risorse statiche SvelteKit
 * 2. SPA fallback: tutte le route client-side → index.html (Quarkus non conosce il router SvelteKit)
 */
@ApplicationScoped
public class CacheHeaders {

    public void register(@Observes Router router) {
        // Cache-Control — eseguito prima di tutto
        router.route("/*").order(-1).handler(ctx -> {
            String path = ctx.request().path();
            String cc = path.startsWith("/_app/immutable/")
                    ? "public, max-age=31536000, immutable"
                    : "no-cache, must-revalidate";
            ctx.response().putHeader("Cache-Control", cc);
            ctx.next();
        });

        // SPA fallback — eseguito dopo i handler API/WS/static (order alto = ultimo)
        // Se il path non ha estensione e non è API/WS → serve index.html (shell SvelteKit)
        router.route("/*").order(Integer.MAX_VALUE - 1).handler(ctx -> {
            String path = ctx.request().path();
            boolean isApi = path.startsWith("/api/") || path.startsWith("/ws/");
            boolean hasExtension = path.lastIndexOf('.') > path.lastIndexOf('/');
            if (!isApi && !hasExtension) {
                ctx.reroute("/index.html");
            } else {
                ctx.next();
            }
        });
    }
}
