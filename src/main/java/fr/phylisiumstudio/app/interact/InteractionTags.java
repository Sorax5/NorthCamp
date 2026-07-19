package fr.phylisiumstudio.app.interact;

import net.minestom.server.tag.Tag;

/**
 * Tags posés sur les entités interactives (NPCs, panneaux, hitbox de slots) pour
 * que le clic droit sache quel menu ouvrir et sur quelle cible.
 */
public final class InteractionTags {
    private InteractionTags() {
    }

    /** Nature de la cible (voir constantes ci-dessous). */
    public static final Tag<String> KIND = Tag.String("nc_kind");
    /** Identifiant de la cible : UUID pour les entités, index pour les slots. */
    public static final Tag<String> ID = Tag.String("nc_id");

    public static final String PLOT = "plot";
    public static final String ACTIVITY = "activity";
    public static final String STAFF = "staff";
    public static final String CLIENT = "client";
    public static final String SLOT_PLOT = "slot_plot";
    public static final String SLOT_ACTIVITY = "slot_activity";
    public static final String SLOT_AMENITY = "slot_amenity";
}
