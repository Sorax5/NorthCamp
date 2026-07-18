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
