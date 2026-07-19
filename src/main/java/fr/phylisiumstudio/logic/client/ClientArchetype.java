package fr.phylisiumstudio.logic.client;

import fr.phylisiumstudio.logic.activity.ActivityType;

/**
 * Profil d'un client, qui oriente ses attentes. Chaque archétype préfère une
 * activité (bonus de satisfaction s'il peut la pratiquer) et tolère plus ou
 * moins un prix au-dessus du marché. Donne au joueur des décisions ciblées :
 * quelles activités construire, quel niveau de prix viser selon la clientèle.
 */
public enum ClientArchetype {
    /** Touriste polyvalent, sans activité fétiche, mais regardant sur le prix. */
    TOURIST("Touriste", null, 1.2),
    /** Pêcheur : vient pour la pêche, plutôt tolérant sur le prix. */
    ANGLER("Pêcheur", ActivityType.FISHING, 0.9),
    /** Baigneur : cherche la baignade. */
    SWIMMER("Baigneur", ActivityType.SWIM, 1.0),
    /** Fêtard : vit pour le barbecue du soir, peu sensible au prix. */
    GRILLER("Fêtard", ActivityType.BARBECUE, 0.7);

    private final String displayName;
    private final ActivityType preferredActivity;
    private final double priceSensitivity;

    ClientArchetype(String displayName, ActivityType preferredActivity, double priceSensitivity) {
        this.displayName = displayName;
        this.preferredActivity = preferredActivity;
        this.priceSensitivity = priceSensitivity;
    }

    public String displayName() {
        return displayName;
    }

    /** Activité préférée, ou {@code null} pour un profil sans préférence. */
    public ActivityType preferredActivity() {
        return preferredActivity;
    }

    /** Multiplicateur appliqué à la pénalité de dépassement de prix (>1 = plus exigeant). */
    public double priceSensitivity() {
        return priceSensitivity;
    }
}
