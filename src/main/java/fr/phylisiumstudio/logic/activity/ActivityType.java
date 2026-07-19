package fr.phylisiumstudio.logic.activity;

/**
 * Type d'activité. Certaines consomment des fournitures à chaque passage (appât
 * pour la pêche, charbon et nourriture pour le barbecue) : elles doivent être
 * ravitaillées et se facturent plus cher au client pour couvrir ce coût. La
 * baignade, elle, ne consomme rien.
 */
public enum ActivityType {
    FISHING("Pêche", TimeAffinity.DAY, 4),
    SWIM("Baignade", TimeAffinity.DAY, 0),
    BARBECUE("Barbecue", TimeAffinity.NIGHT, 8);

    private final String displayName;
    private final TimeAffinity affinity;
    private final int supplyCost;

    ActivityType(String displayName, TimeAffinity affinity, int supplyCost) {
        this.displayName = displayName;
        this.affinity = affinity;
        this.supplyCost = supplyCost;
    }

    public String displayName() {
        return displayName;
    }

    public TimeAffinity affinity() {
        return affinity;
    }

    /** Coût d'achat d'une unité de fourniture (0 = activité sans consommable). */
    public int supplyCost() {
        return supplyCost;
    }

    /** L'activité consomme-t-elle des fournitures à chaque passage ? */
    public boolean consumesSupplies() {
        return supplyCost > 0;
    }
}
