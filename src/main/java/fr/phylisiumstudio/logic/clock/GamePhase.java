package fr.phylisiumstudio.logic.clock;

/**
 * Phase du cycle jour/nuit d'un camping. L'heure Minecraft correspondante est
 * calculée en continu par {@link GameClock#minecraftTime()}.
 */
public enum GamePhase {
    DAY,
    NIGHT;

    public GamePhase next() {
        return this == DAY ? NIGHT : DAY;
    }
}
