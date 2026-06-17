package it.digitaliasistemi.minigames.leaderboard;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;

@Path("/api/leaderboard")
@Produces(MediaType.APPLICATION_JSON)
public class LeaderboardResource {

    @Inject
    LeaderboardService service;

    @GET
    public List<Map<String, Object>> get() {
        return service.getLeaderboard();
    }
}
