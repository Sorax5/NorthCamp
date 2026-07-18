package fr.phylisiumstudio.logic.client;

/**
 * Étape macro du séjour d'un client, pilotée par la boucle de gameplay
 * quotidienne (distincte de {@link Client.ClientState} qui régit la routine
 * minute par minute via l'arbre de comportement).
 */
public enum ClientLifecycle {
    /** Arrivé, patiente à l'accueil en attente d'affectation. */
    WAITING,
    /** Installé sur un emplacement, vit ses vacances. */
    STAYING,
    /** Séjour terminé, quitte le camping. */
    LEAVING,
    /** A quitté le camping (sans avoir été servi, ou fin de séjour). */
    GONE
}
