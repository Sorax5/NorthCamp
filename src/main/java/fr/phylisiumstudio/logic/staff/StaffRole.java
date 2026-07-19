package fr.phylisiumstudio.logic.staff;

/**
 * Domaines de compétence d'un employé. Chaque employé a une aptitude propre
 * (0–1) par rôle : il excelle dans certains et est médiocre dans d'autres.
 */
public enum StaffRole {
    /** Accueil : affecte les clients en attente aux emplacements. */
    RECEPTION("Accueil"),
    /** Nettoyage : remet en état les emplacements sales. */
    CLEANING("Nettoyage"),
    /** Maintenance : entretient les activités pour les garder disponibles. */
    MAINTENANCE("Maintenance"),
    /** Finance : optimise les opérations monétaires. */
    FINANCE("Finance");

    private final String displayName;

    StaffRole(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
