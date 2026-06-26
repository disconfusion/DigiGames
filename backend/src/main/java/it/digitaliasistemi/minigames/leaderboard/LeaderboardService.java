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

    /** Statistiche personali di un utente: totali, per-gioco e ultime partite. */
    public Map<String, Object> userStats(String username) {
        List<MatchResult> all = MatchResult.list("username", username);

        int wins = 0, losses = 0, draws = 0;
        Map<String, int[]> perGame = new LinkedHashMap<>(); // game -> [played, wins, losses, draws]
        for (MatchResult r : all) {
            int[] g = perGame.computeIfAbsent(r.game, k -> new int[4]);
            g[0]++;
            switch (r.result) {
                case "WIN" -> { wins++; g[1]++; }
                case "LOSE" -> { losses++; g[2]++; }
                case "DRAW" -> { draws++; g[3]++; }
                default -> { }
            }
        }

        List<Map<String, Object>> games = new ArrayList<>();
        for (var e : perGame.entrySet()) {
            int[] v = e.getValue();
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("game", e.getKey());
            g.put("played", v[0]);
            g.put("wins", v[1]);
            g.put("losses", v[2]);
            g.put("draws", v[3]);
            games.add(g);
        }
        games.sort((a, b) -> Integer.compare((Integer) b.get("played"), (Integer) a.get("played")));

        List<Map<String, Object>> recent = all.stream()
            .sorted(Comparator.comparing((MatchResult r) -> r.playedAt).reversed())
            .limit(20)
            .map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("game", r.game);
                m.put("result", r.result);
                m.put("playedAt", r.playedAt.toString());
                return m;
            })
            .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", all.size());
        out.put("wins", wins);
        out.put("losses", losses);
        out.put("draws", draws);
        out.put("games", games);
        out.put("recent", recent);
        return out;
    }

    public List<Map<String, Object>> getLeaderboard() {
        List<MatchResult> all = MatchResult.listAll();
        Map<String, UserStats> stats = new LinkedHashMap<>();
        for (MatchResult r : all) {
            UserStats s = stats.computeIfAbsent(r.username, k -> new UserStats(r.username, r.displayName));
            s.total++;
            if ("WIN".equals(r.result)) s.wins++;
            else if ("DRAW".equals(r.result)) s.draws++;
            s.gameStats.computeIfAbsent(r.game, k -> new int[2]);
            s.gameStats.get(r.game)[0]++;
            if ("WIN".equals(r.result)) s.gameStats.get(r.game)[1]++;
        }

        Map<String, String> avatars = new HashMap<>();
        for (AppUser u : AppUser.<AppUser>listAll()) avatars.put(u.username, u.avatar);

        return stats.values().stream()
            .sorted(Comparator.comparingInt((UserStats u) -> u.points()).thenComparingInt(u -> u.wins).reversed())
            .map(u -> {
                Map<String, Object> m = u.toMap();
                m.put("avatar", avatars.get(u.username));
                return m;
            })
            .toList();
    }

    private static class UserStats {
        final String username;
        final String displayName;
        int wins;
        int draws;
        int total;
        final Map<String, int[]> gameStats = new LinkedHashMap<>();

        UserStats(String username, String displayName) {
            this.username = username;
            this.displayName = displayName;
        }

        /** Punti accumulati: vittoria=3, pareggio=1, sconfitta=0. */
        int points() { return wins * 3 + draws; }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", username);
            m.put("displayName", displayName);
            m.put("points", points());
            m.put("wins", wins);
            m.put("draws", draws);
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
