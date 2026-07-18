package fr.phylisiumstudio.logic.economy;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;

/**
 * Fait évoluer la satisfaction des clients et, par ricochet, la réputation du
 * camping. Un client trop insatisfait quitte la file d'attente, entraînant une
 * perte financière et une baisse de réputation.
 */
@Singleton
public class SatisfactionService {

    /** Tolérance de prix : jusqu'à +10 % au-dessus du marché sans pénalité. */
    public static final double PRICE_TOLERANCE = 1.1;
    /** Pénalité de satisfaction par unité de ratio de dépassement du prix juste. */
    private static final double OVERPRICE_PENALTY = 60.0;
    /** Bonus pour une bonne affaire (prix sous le marché). */
    private static final double GOOD_DEAL_BONUS = 10.0;

    private static final double DIRTY_PLOT_PENALTY = 15.0;
    private static final double ACTIVITY_UNAVAILABLE_PENALTY = 12.0;
    private static final double ACTIVITY_ENJOYED_BONUS = 8.0;

    /** En dessous de ce seuil, un client en attente abandonne la file. */
    public static final double ABANDON_THRESHOLD = 30.0;

    /** Facteur de report de la satisfaction finale d'un client sur la réputation. */
    private static final double REPUTATION_DEPARTURE_FACTOR = 0.05;
    /** Baisse de réputation quand un client quitte la file d'attente. */
    private static final double REPUTATION_ABANDON_PENALTY = 2.0;

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }

    private void adjust(Client client, double delta) {
        client.setSatisfaction(clamp(client.getSatisfaction() + delta));
    }

    /**
     * Applique l'effet du prix demandé (via le ratio marché) sur la satisfaction.
     *
     * @param priceRatio prix demandé / prix juste du marché
     */
    public void evaluatePricing(Client client, double priceRatio) {
        if (priceRatio <= 1.0) {
            adjust(client, GOOD_DEAL_BONUS * (1.0 - priceRatio));
        } else if (priceRatio > PRICE_TOLERANCE) {
            adjust(client, -OVERPRICE_PENALTY * (priceRatio - PRICE_TOLERANCE));
        }
    }

    public void penalizeDirtyPlot(Client client) {
        adjust(client, -DIRTY_PLOT_PENALTY);
    }

    public void penalizeUnavailableActivity(Client client) {
        adjust(client, -ACTIVITY_UNAVAILABLE_PENALTY);
    }

    public void rewardActivityEnjoyed(Client client) {
        adjust(client, ACTIVITY_ENJOYED_BONUS);
    }

    /** Un client en attente abandonne-t-il faute de satisfaction suffisante ? */
    public boolean shouldAbandonQueue(Client client) {
        return client.getSatisfaction() < ABANDON_THRESHOLD;
    }

    /**
     * Reporte la satisfaction finale d'un client qui part sur la réputation du
     * camping (au-dessus de 50 = amélioration, en dessous = dégradation).
     */
    public void applyDeparture(Campsite campsite, Client client) {
        campsite.adjustReputation((client.getSatisfaction() - 50.0) * REPUTATION_DEPARTURE_FACTOR);
    }

    /** Applique la pénalité de réputation liée à un abandon de file. */
    public void applyQueueAbandonment(Campsite campsite) {
        campsite.adjustReputation(-REPUTATION_ABANDON_PENALTY);
    }
}
