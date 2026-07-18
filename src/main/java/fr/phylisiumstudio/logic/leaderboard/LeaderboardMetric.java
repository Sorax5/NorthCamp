package fr.phylisiumstudio.logic.leaderboard;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;

import java.util.function.ToDoubleFunction;

/**
 * Statistiques sur lesquelles les joueurs s'affrontent dans le classement global.
 * Chaque métrique sait extraire sa valeur d'un camping.
 */
public enum LeaderboardMetric {
    /** Revenus générés (solde du camping). */
    REVENUE("Revenus", Campsite::getMoney),
    /** Réputation du camping (0–100). */
    REPUTATION("Réputation", Campsite::getReputation),
    /** Satisfaction client moyenne (0–100). */
    SATISFACTION("Satisfaction", LeaderboardMetric::averageSatisfaction);

    private final String displayName;
    private final ToDoubleFunction<Campsite> extractor;

    LeaderboardMetric(String displayName, ToDoubleFunction<Campsite> extractor) {
        this.displayName = displayName;
        this.extractor = extractor;
    }

    public String displayName() {
        return displayName;
    }

    public double score(Campsite campsite) {
        return extractor.applyAsDouble(campsite);
    }

    private static double averageSatisfaction(Campsite campsite) {
        return campsite.getClients().stream()
                .mapToDouble(Client::getSatisfaction)
                .average()
                .orElse(0.0);
    }
}
