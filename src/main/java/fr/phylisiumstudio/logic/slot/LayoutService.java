package fr.phylisiumstudio.logic.slot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.marker.MarkerSet;
import fr.phylisiumstudio.logic.marker.MarkerTags;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import net.hollowcube.schem.util.Rotation;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fournit les positions des emplacements constructibles de la carte.
 *
 * <p>Les positions proviennent des marqueurs {@code plot_slot} / {@code activity_slot}
 * d'un schématic de disposition (« layout »). En son absence, une grille par défaut
 * est générée pour rester jouable.
 */
@Singleton
public class LayoutService {
    private static final Logger logger = LoggerFactory.getLogger(LayoutService.class);

    private static final String LAYOUT_SCHEMATIC = "layout.nbt";
    private static final Vector3d ORIGIN = new Vector3d(0, 69, 0);

    // Grille par défaut si aucun layout n'est fourni.
    private static final int DEFAULT_PLOT_SLOTS = 12;
    private static final int DEFAULT_ACTIVITY_SLOTS = 4;
    private static final int GRID_COLUMNS = 6;
    private static final int SPACING = 12;

    private final SchematicFactory schematicFactory;

    private final List<Vector3d> plotSlots = new ArrayList<>();
    private final List<Vector3d> activitySlots = new ArrayList<>();

    @Inject
    public LayoutService(SchematicFactory schematicFactory) {
        this.schematicFactory = schematicFactory;
    }

    /** Charge les positions de slots. À appeler après le chargement des schématics. */
    public void load() {
        plotSlots.clear();
        activitySlots.clear();

        var layout = tryGetLayout();
        if (layout != null) {
            var markers = MarkerSet.resolve(layout, ORIGIN, Rotation.NONE);
            plotSlots.addAll(markers.all(MarkerTags.PLOT_SLOT));
            activitySlots.addAll(markers.all(MarkerTags.ACTIVITY_SLOT));
        }

        if (plotSlots.isEmpty()) {
            plotSlots.addAll(defaultGrid(DEFAULT_PLOT_SLOTS, ORIGIN));
        }
        if (activitySlots.isEmpty()) {
            activitySlots.addAll(defaultGrid(DEFAULT_ACTIVITY_SLOTS, new Vector3d(ORIGIN).add(0, 0, -20)));
        }

        logger.info("Layout loaded: {} plot slots, {} activity slots", plotSlots.size(), activitySlots.size());
    }

    public List<Vector3d> plotSlotPositions() {
        return List.copyOf(plotSlots);
    }

    public List<Vector3d> activitySlotPositions() {
        return List.copyOf(activitySlots);
    }

    private net.hollowcube.schem.Schematic tryGetLayout() {
        try {
            return schematicFactory.getSchematic(LAYOUT_SCHEMATIC);
        } catch (Exception e) {
            return null; // pas de layout fourni : grille par défaut
        }
    }

    private List<Vector3d> defaultGrid(int count, Vector3d origin) {
        var positions = new ArrayList<Vector3d>(count);
        for (int i = 0; i < count; i++) {
            int row = i / GRID_COLUMNS;
            int col = i % GRID_COLUMNS;
            positions.add(new Vector3d(origin).add(col * SPACING, 0, row * SPACING));
        }
        return positions;
    }
}
