package it.digitaliasistemi.minigames.leaderboard;

import it.digitaliasistemi.minigames.domain.AppUser;
import it.digitaliasistemi.minigames.domain.MatchResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class LeaderboardService {

    @Transactional
    public void record(String username, String game, String result) {
        AppUser user = AppUser.findByUsername(username);
        MatchResult r = new MatchResult();
        r.username = username;
        r.displayName = user != null ? user.displayName : username;
        r.game = game;
        r.result = result;
        r.playedAt = Instant.now();
        r.persist();
    }

    public List<Map<String, Object>> getLeaderboard() {
        List<MatchResult> all = MatchResult.listAll();
        Map<String, UserStats> stats = new LinkedHashMap<>();
        for (MatchResult r : all) {
            UserStats s = stats.computeIfAbsent(r.username, k -> new UserStats(r.username, r.displayName));
            s.total++;
            if ("WIN".equals(r.result)) s.wins++;
            s.gameStats.computeIfAbsent(r.game, k -> new int[2]);
            s.gameStats.get(r.game)[0]++;
            if ("WIN".equals(r.result)) s.gameStats.get(r.game)[1]++;
        }
        return stats.values().stream()
            .sorted(Comparator.comparingInt((UserStats u) -> u.wins).reversed())
            .map(UserStats::toMap)
            .toList();
    }

    private static class UserStats {
        final String username;
        final String displayName;
        int wins;
        int total;
        final Map<String, int[]> gameStats = new LinkedHashMap<>();

        UserStats(String username, String displayName) {
            this.username = username;
            this.displayName = displayName;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", username);
            m.put("displayName", displayName);
            m.put("wins", wins);
            m.put("total", total);
            Map<String, Object> games = new LinkedHashMap<>();
            for (var e : gameStats.entrySet()) {
                Map<String, Object> g = new LinkedHashMap<>();
                g.put("played", e.getValue()[0]);
                g.put("wins", e.getValue()[1]);
                games.put(e.getKey(), g);
            }
            m.put("games", games);
            return m;
        }
    }
}
