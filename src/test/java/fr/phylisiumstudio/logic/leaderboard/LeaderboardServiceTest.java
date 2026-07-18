package fr.phylisiumstudio.logic.leaderboard;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.repository.ICampsiteRepository;
import fr.phylisiumstudio.logic.service.CampsiteService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardServiceTest {

    /** Repository minimal en mémoire, sans dépendance de mock. */
    private static class FakeCampsiteRepository implements ICampsiteRepository {
        public CompletableFuture<Campsite> create(Campsite e) { return CompletableFuture.completedFuture(e); }
        public CompletableFuture<Campsite> read(UUID id) { return CompletableFuture.completedFuture(null); }
        public CompletableFuture<Campsite> update(Campsite e) { return CompletableFuture.completedFuture(e); }
        public CompletableFuture<Void> delete(UUID id) { return CompletableFuture.completedFuture(null); }
        public CompletableFuture<List<Campsite>> list() { return CompletableFuture.completedFuture(List.of()); }
        public CompletableFuture<Boolean> exists(UUID id) { return CompletableFuture.completedFuture(false); }
    }

    private CampsiteService campsiteServiceWith(List<Campsite> campsites) {
        var service = new CampsiteService(new FakeCampsiteRepository());
        campsites.forEach(service::addCampsite);
        return service;
    }

    private static Campsite withMoney(double money) {
        var c = new Campsite(UUID.randomUUID());
        c.addMoney(money);
        return c;
    }

    @Test
    void ranksCampsitesByRevenueDescending() {
        var poor = withMoney(100);
        var rich = withMoney(5000);
        var mid = withMoney(1000);
        var leaderboard = new LeaderboardService(campsiteServiceWith(List.of(poor, rich, mid)));

        var top = leaderboard.top(LeaderboardMetric.REVENUE, 10);

        assertEquals(3, top.size());
        assertEquals(1, top.get(0).rank());
        assertEquals(rich.getUniqueID(), top.get(0).campsiteID());
        assertEquals(mid.getUniqueID(), top.get(1).campsiteID());
        assertEquals(poor.getUniqueID(), top.get(2).campsiteID());
    }

    @Test
    void limitCapsResults() {
        var leaderboard = new LeaderboardService(
                campsiteServiceWith(List.of(withMoney(1), withMoney(2), withMoney(3))));
        assertEquals(2, leaderboard.top(LeaderboardMetric.REVENUE, 2).size());
    }

    @Test
    void rankOfReturnsPlayerPosition() {
        var rich = withMoney(5000);
        var poor = withMoney(100);
        var leaderboard = new LeaderboardService(campsiteServiceWith(List.of(poor, rich)));

        assertEquals(1, leaderboard.rankOf(rich.getOwnerID(), LeaderboardMetric.REVENUE).orElseThrow());
        assertEquals(2, leaderboard.rankOf(poor.getOwnerID(), LeaderboardMetric.REVENUE).orElseThrow());
        assertTrue(leaderboard.rankOf(UUID.randomUUID(), LeaderboardMetric.REVENUE).isEmpty());
    }
}
