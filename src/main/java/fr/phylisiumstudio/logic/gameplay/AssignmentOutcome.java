package fr.phylisiumstudio.logic.gameplay;

/**
 * Résultat de la tentative d'affectation d'un client à un emplacement.
 */
public enum AssignmentOutcome {
    SUCCESS,
    /** Le client n'attend pas d'affectation (déjà installé ou parti). */
    CLIENT_NOT_WAITING,
    /** L'emplacement est déjà occupé par un autre séjour. */
    PLOT_OCCUPIED,
    /** L'emplacement est sale et doit être nettoyé avant réutilisation. */
    PLOT_DIRTY,
    /** Un groupe/famille exige un emplacement de niveau supérieur. */
    NEEDS_HIGHER_LEVEL;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
