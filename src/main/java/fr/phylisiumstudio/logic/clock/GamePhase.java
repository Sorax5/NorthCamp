package fr.phylisiumstudio.logic.clock;

/**
 * Phase du cycle jour/nuit d'un camping.
 *
 * <p>{@code minecraftTime} est le tick de temps Minecraft représentatif de la phase,
 * appliqué à l'instance pour le rendu visuel du ciel.
 */
public enum GamePhase {
    DAY(1_000L),
    NIGHT(13_000L);

    private final long minecraftTime;

    GamePhase(long minecraftTime) {
        this.minecraftTime = minecraftTime;
    }

    public long minecraftTime() {
        return minecraftTime;
    }

    public GamePhase next() {
        return this == DAY ? NIGHT : DAY;
    }
}
