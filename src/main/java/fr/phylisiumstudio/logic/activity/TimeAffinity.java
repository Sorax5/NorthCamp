package fr.phylisiumstudio.logic.activity;

/**
 * Moment de la journée où une activité est la plus appréciée. Sert à orienter
 * (sans forcer) le choix d'activité des clients selon l'heure.
 */
public enum TimeAffinity {
    /** Plutôt en journée (baignade, pêche…). */
    DAY,
    /** Plutôt en soirée/nuit (barbecue…). */
    NIGHT,
    /** Sans préférence marquée. */
    ANY
}
