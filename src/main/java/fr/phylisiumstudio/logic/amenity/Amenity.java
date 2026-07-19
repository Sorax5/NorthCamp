package fr.phylisiumstudio.logic.amenity;

/**
 * Service à l'échelle du camping, construit une fois et bénéficiant à tous les
 * campeurs. Chaque aménagement présent apporte un bonus de confort quotidien à la
 * satisfaction des clients en séjour : une raison d'investir au-delà des seuls
 * emplacements, dans le thème du camping nord-américain.
 */
public enum Amenity {
    SHOWERS("Sanitaires", 2_000),
    SHOP("Épicerie", 3_000),
    WIFI("Wi-Fi", 1_500),
    PLAYGROUND("Aire de jeux", 2_500),
    LAUNDRY("Laverie", 2_000);

    private final String displayName;
    private final long cost;

    Amenity(String displayName, long cost) {
        this.displayName = displayName;
        this.cost = cost;
    }

    public String displayName() {
        return displayName;
    }

    public long cost() {
        return cost;
    }
}
