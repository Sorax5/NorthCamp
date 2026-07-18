package fr.phylisiumstudio.logic.marker;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.util.Rotation;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Positions de marqueurs résolues en coordonnées monde pour un schématic posé.
 *
 * <p>Fournit un accès par tag avec repli sur une position par défaut lorsque le
 * tag est absent, afin que le gameplay fonctionne même sans marqueurs.
 */
public final class MarkerSet {

    private final Map<String, List<Vector3d>> markers;

    private MarkerSet(Map<String, List<Vector3d>> markers) {
        this.markers = markers;
    }

    /**
     * Résout les marqueurs d'un schématic posé à {@code base}.
     *
     * <p>La rotation de base ({@link Rotation#NONE}) est conservée : les positions
     * sont simplement translatées. Toute autre rotation n'est pas supportée ici
     * (le placement du camping conserve la rotation de base).
     */
    public static MarkerSet resolve(Schematic schematic, Vector3d base, Rotation rotation) {
        var relative = SchematicMarkers.parse(schematic);
        var resolved = new HashMap<String, List<Vector3d>>();
        for (var entry : relative.entrySet()) {
            var worldPositions = new ArrayList<Vector3d>(entry.getValue().size());
            for (var offset : entry.getValue()) {
                worldPositions.add(new Vector3d(base).add(offset));
            }
            resolved.put(entry.getKey(), worldPositions);
        }
        return new MarkerSet(resolved);
    }

    /** Ensemble vide (aucun marqueur) : tout se rabat sur les défauts. */
    public static MarkerSet empty() {
        return new MarkerSet(new HashMap<>());
    }

    /** Toutes les positions pour un tag (liste vide si absent). */
    public List<Vector3d> all(String tag) {
        return markers.getOrDefault(tag, List.of());
    }

    /** Première position pour un tag, si présente. */
    public Optional<Vector3d> first(String tag) {
        var list = markers.get(tag);
        return (list == null || list.isEmpty()) ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Première position pour un tag, ou la valeur par défaut fournie. */
    public Vector3d firstOr(String tag, Vector3d fallback) {
        return first(tag).orElse(fallback);
    }

    public boolean has(String tag) {
        return markers.containsKey(tag) && !markers.get(tag).isEmpty();
    }
}
