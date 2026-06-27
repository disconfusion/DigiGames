package it.digitaliasistemi.minigames.house;

import it.digitaliasistemi.minigames.domain.UserHouse;
import it.digitaliasistemi.minigames.leaderboard.LeaderboardService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Logica delle casate: casata dell'utente, scelta, classifica casate. */
@ApplicationScoped
public class HouseService {

    @Inject LeaderboardService leaderboard;

    /** id della casata dell'utente, o null. */
    public String houseOf(String username) {
        UserHouse h = UserHouse.forUsername(username);
        return h != null ? h.house : null;
    }

    /** Sceglie/cambia la casata (deve essere valida). */
    @Transactional
    public boolean setHouse(String username, String houseId) {
        if (!HouseCatalog.isValid(houseId)) return false;
        UserHouse h = UserHouse.forUsername(username);
        if (h == null) {
            h = new UserHouse();
            h.username = username;
            h.house = houseId;
            h.persist();
        } else {
            h.house = houseId;
        }
        return true;
    }

    /** Classifica casate: somma dei punti dei membri + numero membri. */
    public List<Map<String, Object>> standings() {
        Map<String, Integer> pts = new HashMap<>();
        for (Map<String, Object> row : leaderboard.getLeaderboard()) {
            pts.put((String) row.get("username"), (Integer) row.get("points"));
        }
        Map<String, int[]> agg = new LinkedHashMap<>(); // houseId -> [points, members]
        for (HouseCatalog.HouseDef h : HouseCatalog.HOUSES) agg.put(h.id(), new int[2]);
        for (UserHouse uh : UserHouse.<UserHouse>listAll()) {
            int[] a = agg.get(uh.house);
            if (a == null) continue;
            a[0] += pts.getOrDefault(uh.username, 0);
            a[1] += 1;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (HouseCatalog.HouseDef h : HouseCatalog.HOUSES) {
            int[] a = agg.get(h.id());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.id());
            m.put("name", h.name());
            m.put("points", a[0]);
            m.put("members", a[1]);
            out.add(m);
        }
        out.sort((x, y) -> Integer.compare((Integer) y.get("points"), (Integer) x.get("points")));
        return out;
    }
}
