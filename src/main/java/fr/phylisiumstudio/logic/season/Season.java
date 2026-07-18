package fr.phylisiumstudio.logic.season;

/**
 * Saison de l'année de jeu. Chaque saison module l'affluence : l'été attire des
 * pics de visiteurs, l'hiver les raréfie. Le joueur doit s'y préparer.
 */
public enum Season {
    SPRING("Printemps", 1.1),
    SUMMER("Été", 1.6),
    AUTUMN("Automne", 0.9),
    WINTER("Hiver", 0.5);

    private final String displayName;
    private final double arrivalMultiplier;

    Season(String displayName, double arrivalMultiplier) {
        this.displayName = displayName;
        this.arrivalMultiplier = arrivalMultiplier;
    }

    public String displayName() {
        return displayName;
    }

    public double arrivalMultiplier() {
        return arrivalMultiplier;
    }
}
