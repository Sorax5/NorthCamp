package fr.phylisiumstudio.logic.client;

/**
 * Étape macro du séjour d'un client, pilotée par la boucle de gameplay
 * quotidienne (distincte de {@link Client.ClientState} qui régit la routine
 * minute par minute via l'arbre de comportement).
 */
public enum ClientLifecycle {
    /** Arrivé, patiente à l'accueil en attente d'affectation. */
    WAITING("En attente"),
    /** Installé sur un emplacement, vit ses vacances. */
    STAYING("En séjour"),
    /** Séjour terminé, quitte le camping. */
    LEAVING("En départ"),
    /** A quitté le camping (sans avoir été servi, ou fin de séjour). */
    GONE("Parti");

    private final String displayName;

    ClientLifecycle(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
