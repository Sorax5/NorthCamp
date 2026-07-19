package fr.phylisiumstudio.logic.amenity;

/**
 * Service à l'échelle du camping, construit une fois et bénéficiant à tous les
 * campeurs. Chaque aménagement présent apporte un bonus de confort quotidien à la
 * satisfaction des clients en séjour : une raison d'investir au-delà des seuls
 * emplacements, dans le thème du camping nord-américain.
 */
public enum Amenity {
    SHOWERS("Sanitaires", 2_000, "showers.nbt"),
    SHOP("Épicerie", 3_000, "shop.nbt"),
    WIFI("Wi-Fi", 1_500, "wifi.nbt"),
    PLAYGROUND("Aire de jeux", 2_500, "playground.nbt"),
    LAUNDRY("Laverie", 2_000, "laundry.nbt");

    private final String displayName;
    private final long cost;
    private final String schem;

    Amenity(String displayName, long cost, String schem) {
        this.displayName = displayName;
        this.cost = cost;
        this.schem = schem;
    }

    public String displayName() {
        return displayName;
    }

    public long cost() {
        return cost;
    }

    /** Fichier schématique (.nbt dans run/schem) posé lors de la construction. */
    public String schem() {
        return schem;
    }
}
