package fr.phylisiumstudio.logic.marker;

/**
 * Tags conventionnels posés sur les armor stands des schématics pour marquer des
 * positions fonctionnelles (points de repère). Un builder de map place ces armor
 * stands taggés ; le jeu les lit à la pose du schématic.
 */
public final class MarkerTags {
    private MarkerTags() {
    }

    // ── Emplacements (plots) ────────────────────────────────────────────────
    /** Endroit où le client dort. Peut apparaître plusieurs fois. */
    public static final String SLEEP = "sleep";
    /** Endroit où le client s'assoit. Peut apparaître plusieurs fois. */
    public static final String SIT = "sit";
    /** Point où le NPC apparaît avant de rejoindre l'accueil. */
    public static final String NPC_SPAWN = "npc_spawn";

    /** Point de sortie : le client s'y rend en fin de séjour avant de disparaître. */
    public static final String EXIT = "exit";

    // ── Accueil global ──────────────────────────────────────────────────────
    /** Comptoir d'accueil où patientent les clients. */
    public static final String RECEPTION = "reception";

    /** Emplacement du panneau d'information (text display) du lieu. */
    public static final String INFO = "info";

    // ── Activités ───────────────────────────────────────────────────────────
    /** Point où le client se rend pour pratiquer l'activité. */
    public static final String ACTIVITY_TARGET = "activity_target";

    // ── Personnel ───────────────────────────────────────────────────────────
    /** Poste de l'employé d'accueil. */
    public static final String STAFF_RECEPTION = "staff_reception";
    /** Poste de l'employé de maintenance. */
    public static final String STAFF_MAINTENANCE = "staff_maintenance";
    /** Salle de pause du personnel. */
    public static final String STAFF_BREAK = "staff_break";

    // ── Découverte d'emplacements achetables ────────────────────────────────
    /** Emplacement de plot disponible à l'achat (non encore défini). */
    public static final String PLOT_SLOT = "plot_slot";
    /** Emplacement d'activité disponible à l'achat (non encore défini). */
    public static final String ACTIVITY_SLOT = "activity_slot";
}
