package fr.phylisiumstudio.logic.plot;

/**
 * Type d'emplacement, avec son caractère propre. Le type module la durée de
 * séjour du client installé et lui procure un bonus de satisfaction à
 * l'installation — de quoi différencier les stratégies :
 *
 * <ul>
 *   <li><b>Tente</b> : séjours courts (turn-over rapide) mais grande joie du
 *       plein air.</li>
 *   <li><b>Caravane</b> : séjours longs (revenus étalés) au confort plus posé.</li>
 * </ul>
 */
public enum PlotType {
    CAMPSITE("Tente", 0.7, 8.0),
    CARAVAN("Caravane", 1.4, 4.0);

    private final String displayName;
    private final double stayMultiplier;
    private final double comfortBonus;

    PlotType(String displayName, double stayMultiplier, double comfortBonus) {
        this.displayName = displayName;
        this.stayMultiplier = stayMultiplier;
        this.comfortBonus = comfortBonus;
    }

    public String displayName() {
        return displayName;
    }

    /** Multiplicateur appliqué à la durée de séjour à l'installation (<1 court, >1 long). */
    public double stayMultiplier() {
        return stayMultiplier;
    }

    /** Bonus de satisfaction accordé une fois, au moment de l'installation. */
    public double comfortBonus() {
        return comfortBonus;
    }
}
