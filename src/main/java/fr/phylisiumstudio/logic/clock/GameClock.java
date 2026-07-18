package fr.phylisiumstudio.logic.clock;

import lombok.Getter;

/**
 * Horloge de jeu pure (sans dépendance moteur) pilotant le cycle jour/nuit
 * d'un camping. Un « tick » représente une seconde réelle écoulée.
 *
 * <p>Séparée des effets de bord (rendu, événements) pour rester testable : le
 * service appelant réagit aux transitions retournées par {@link #tick()}.
 */
@Getter
public class GameClock {
    private final int dayDurationSeconds;
    private final int nightDurationSeconds;

    private GamePhase phase = GamePhase.DAY;
    private long dayNumber = 1;
    private int secondsInPhase = 0;

    public GameClock(int dayDurationSeconds, int nightDurationSeconds) {
        if (dayDurationSeconds <= 0 || nightDurationSeconds <= 0) {
            throw new IllegalArgumentException("Phase durations must be positive");
        }
        this.dayDurationSeconds = dayDurationSeconds;
        this.nightDurationSeconds = nightDurationSeconds;
    }

    private int currentDuration() {
        return phase == GamePhase.DAY ? dayDurationSeconds : nightDurationSeconds;
    }

    /**
     * Heure Minecraft (0–23999 ticks) reflétant en continu la progression de
     * l'horloge : le jeu fait avancer le ciel proportionnellement au temps écoulé
     * dans la phase, pour que le ciel suive réellement le cycle interne.
     *
     * <p>Jour → 0..12000, nuit → 12000..24000.
     */
    public long minecraftTime() {
        int duration = currentDuration();
        double fraction = duration <= 0 ? 0.0 : Math.min(1.0, (double) secondsInPhase / duration);
        long base = phase == GamePhase.DAY ? 0L : 12_000L;
        return (base + Math.round(fraction * 12_000.0)) % 24_000L;
    }

    /**
     * Avance l'horloge d'une seconde.
     *
     * @return {@code true} si une transition de phase vient de se produire.
     */
    public boolean tick() {
        secondsInPhase++;
        if (secondsInPhase < currentDuration()) {
            return false;
        }
        phase = phase.next();
        if (phase == GamePhase.DAY) {
            dayNumber++;
        }
        secondsInPhase = 0;
        return true;
    }
}
