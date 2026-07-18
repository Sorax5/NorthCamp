package fr.phylisiumstudio.logic.season;

import com.google.inject.Singleton;

/**
 * Dérive la saison courante du numéro de jour et fournit le multiplicateur
 * d'affluence associé, plus d'éventuels événements de pic ponctuels.
 */
@Singleton
public class SeasonService {

    /** Nombre de jours de jeu par saison. */
    public static final int DAYS_PER_SEASON = 7;
    /** Tous les N jours, un événement spécial dope encore l'affluence. */
    private static final int SPECIAL_EVENT_INTERVAL = 10;
    private static final double SPECIAL_EVENT_MULTIPLIER = 1.5;

    /** Saison correspondant à un jour donné (jour 1 = premier jour du printemps). */
    public Season seasonOf(long dayNumber) {
        long index = ((dayNumber - 1) / DAYS_PER_SEASON) % Season.values().length;
        return Season.values()[(int) index];
    }

    /** Un jour d'événement spécial (pic de visiteurs à préparer) ? */
    public boolean isSpecialEvent(long dayNumber) {
        return dayNumber % SPECIAL_EVENT_INTERVAL == 0;
    }

    /** Multiplicateur d'affluence combinant saison et événement spécial. */
    public double arrivalMultiplier(long dayNumber) {
        double multiplier = seasonOf(dayNumber).arrivalMultiplier();
        if (isSpecialEvent(dayNumber)) {
            multiplier *= SPECIAL_EVENT_MULTIPLIER;
        }
        return multiplier;
    }
}
