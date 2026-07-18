package fr.phylisiumstudio.logic.leaderboard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.service.CampsiteService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Calcule le classement mondial inter-joueurs sur une métrique donnée
 * (revenus, réputation, satisfaction). Les campings sont classés du meilleur
 * au moins bon.
 */
@Singleton
public class LeaderboardService {

    private final CampsiteService campsiteService;

    @Inject
    public LeaderboardService(CampsiteService campsiteService) {
        this.campsiteService = campsiteService;
    }

    /** Les {@code limit} meilleurs campings sur la métrique, du 1er au dernier. */
    public List<LeaderboardEntry> top(LeaderboardMetric metric, int limit) {
        var sorted = campsiteService.getCampsites().values().stream()
                .sorted(Comparator.comparingDouble(metric::score).reversed())
                .limit(Math.max(0, limit))
                .toList();

        var rank = new AtomicInteger(1);
        return sorted.stream()
                .map(campsite -> new LeaderboardEntry(
                        rank.getAndIncrement(),
                        campsite.getOwnerID(),
                        campsite.getUniqueID(),
                        metric.score(campsite)))
                .toList();
    }

    /** Rang (1 = meilleur) d'un joueur donné sur la métrique, s'il possède un camping. */
    public Optional<Integer> rankOf(UUID ownerID, LeaderboardMetric metric) {
        var ordered = campsiteService.getCampsites().values().stream()
                .sorted(Comparator.comparingDouble(metric::score).reversed())
                .toList();

        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getOwnerID().equals(ownerID)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }
}
