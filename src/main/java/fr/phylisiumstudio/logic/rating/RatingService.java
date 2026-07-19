package fr.phylisiumstudio.logic.rating;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;

/**
 * Note globale du camping en étoiles (0–5), combinant réputation et satisfaction
 * moyenne des clients. C'est la « note » que le joueur cherche à maximiser, en
 * parallèle de l'argent. Franchir un nouveau palier d'étoiles est récompensé.
 */
@Singleton
public class RatingService {

    public static final int MAX_STARS = 5;
    /** Majoration de prix par étoile : un camping mieux noté peut facturer plus cher. */
    public static final double STAR_PRICE_BONUS = 0.08;

    /** Note en étoiles du camping (0–5). Sans client, se fonde sur la seule réputation. */
    public int stars(Campsite campsite) {
        return ratingOf(campsite);
    }

    /** Variante statique (pour les vues sans injection, ex. sidebar). */
    public static int ratingOf(Campsite campsite) {
        double base = campsite.getClients().isEmpty()
                ? campsite.getReputation()
                : (campsite.getReputation() + averageSatisfaction(campsite)) / 2.0;
        return starsOf(base);
    }

    /** Convertit un score 0–100 en étoiles 0–5 (paliers 20/40/60/75/90). */
    public static int starsOf(double score) {
        if (score >= 90) return 5;
        if (score >= 75) return 4;
        if (score >= 60) return 3;
        if (score >= 40) return 2;
        if (score >= 20) return 1;
        return 0;
    }

    public static double averageSatisfaction(Campsite campsite) {
        return campsite.getClients().stream()
                .mapToDouble(Client::getSatisfaction)
                .average()
                .orElse(0.0);
    }

    /**
     * Multiplicateur de prix lié à la note : les loyers et revenus d'activité
     * grimpent avec l'étoile courante ({@code 1.0} à 0★, jusqu'à {@code 1.40} à 5★).
     */
    public static double priceMultiplier(Campsite campsite) {
        // Brevet « Marque premium » : bonus de prix par étoile renforcé.
        double perStar = STAR_PRICE_BONUS
                + (campsite.hasPatent(fr.phylisiumstudio.logic.vendor.Patent.PREMIUM_BRAND) ? 0.04 : 0.0);
        return 1.0 + ratingOf(campsite) * perStar;
    }

    /** Rendu texte « ★★★☆☆ » pour un nombre d'étoiles donné. */
    public static String render(int stars) {
        int s = Math.max(0, Math.min(MAX_STARS, stars));
        return "★".repeat(s) + "☆".repeat(MAX_STARS - s);
    }
}
