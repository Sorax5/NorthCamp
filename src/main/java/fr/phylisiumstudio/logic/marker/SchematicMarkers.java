package fr.phylisiumstudio.logic.marker;

import net.hollowcube.schem.Schematic;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extrait les positions marquées par des armor stands taggés à l'intérieur d'un
 * schématic. Chaque entité taggée contribue sa position (relative à l'origine du
 * schématic) à chacun de ses tags.
 *
 * <p>Robuste aux deux dispositions NBT rencontrées : données d'entité au niveau
 * racine (format Sponge) ou imbriquées sous {@code nbt} (format Structure).
 */
public final class SchematicMarkers {
    private SchematicMarkers() {
    }

    /**
     * Lit les marqueurs d'un schématic.
     *
     * @return map tag → positions relatives (à l'origine du schématic).
     */
    public static Map<String, List<Vector3d>> parse(Schematic schematic) {
        Map<String, List<Vector3d>> markers = new HashMap<>();
        for (var entity : schematic.entities()) {
            var data = entityData(entity);
            var tags = data.getList("Tags", BinaryTagTypes.STRING);
            if (tags.isEmpty()) {
                continue;
            }
            var position = readPosition(entity, data);
            if (position == null) {
                continue;
            }
            for (var tag : tags.stream().toList()) {
                var name = ((net.kyori.adventure.nbt.StringBinaryTag) tag).value();
                markers.computeIfAbsent(name, _ -> new ArrayList<>()).add(position);
            }
        }
        return markers;
    }

    /** Données d'entité : sous-compound {@code nbt} si présent, sinon l'entité elle-même. */
    private static CompoundBinaryTag entityData(CompoundBinaryTag entity) {
        var nested = entity.getCompound("nbt");
        return nested.keySet().isEmpty() ? entity : nested;
    }

    /** Lit la position depuis {@code Pos} ou {@code pos} (liste de 3 doubles). */
    private static Vector3d readPosition(CompoundBinaryTag entity, CompoundBinaryTag data) {
        var pos = firstNonEmpty(
                entity.getList("Pos", BinaryTagTypes.DOUBLE),
                entity.getList("pos", BinaryTagTypes.DOUBLE),
                data.getList("Pos", BinaryTagTypes.DOUBLE),
                data.getList("pos", BinaryTagTypes.DOUBLE));
        if (pos == null || pos.size() < 3) {
            return null;
        }
        return new Vector3d(pos.getDouble(0), pos.getDouble(1), pos.getDouble(2));
    }

    private static ListBinaryTag firstNonEmpty(ListBinaryTag... lists) {
        for (var list : lists) {
            if (list != null && !list.isEmpty()) {
                return list;
            }
        }
        return null;
    }
}
