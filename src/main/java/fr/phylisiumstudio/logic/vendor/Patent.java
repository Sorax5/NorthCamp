package fr.phylisiumstudio.logic.vendor;

/**
 * Brevet : amélioration globale permanente du camping, vendue uniquement par les
 * marchands ambulants qui passent à l'entrée. Acheté une fois, son effet
 * s'applique en continu.
 */
public enum Patent {
    ECO_SUPPLIES("Fournitures éco", "Coût des fournitures d'activité −30 %", 6_000),
    PREMIUM_BRAND("Marque premium", "Premium étoiles renforcé sur les loyers/revenus", 8_000),
    EFFICIENT_STAFF("Staff efficace", "+1 tâche par employé et par jour", 7_000),
    MARKETING("Marketing", "+20 % de chance d'arrivée de clients", 5_000),
    COMFORT_PLUS("Confort +", "+50 % d'effet confort des services", 6_000);

    private final String displayName;
    private final String description;
    private final long cost;

    Patent(String displayName, String description, long cost) {
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public long cost() {
        return cost;
    }
}
