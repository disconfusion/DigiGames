package it.digitaliasistemi.minigames.game;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Registry dei giochi: raccoglie tutti i bean GameEngine e li indicizza per slug. */
@ApplicationScoped
public class GameEngines {

    private final Map<String, GameEngine> bySlug = new HashMap<>();

    @Inject
    public GameEngines(Instance<GameEngine> engines) {
        for (GameEngine e : engines) {
            bySlug.put(e.slug(), e);
        }
    }

    public GameEngine get(String slug) {
        return slug == null ? null : bySlug.get(slug);
    }

    public boolean isKnown(String slug) {
        return bySlug.containsKey(slug);
    }

    public Set<String> slugs() {
        return bySlug.keySet();
    }
}
