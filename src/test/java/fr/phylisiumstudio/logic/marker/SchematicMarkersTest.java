package fr.phylisiumstudio.logic.marker;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.util.BlockConsumer;
import net.hollowcube.schem.util.Rotation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchematicMarkersTest {

    private static ListBinaryTag pos(double x, double y, double z) {
        return ListBinaryTag.builder(net.kyori.adventure.nbt.BinaryTagTypes.DOUBLE)
                .add(DoubleBinaryTag.doubleBinaryTag(x))
                .add(DoubleBinaryTag.doubleBinaryTag(y))
                .add(DoubleBinaryTag.doubleBinaryTag(z))
                .build();
    }

    private static ListBinaryTag tags(String... names) {
        var builder = ListBinaryTag.builder(net.kyori.adventure.nbt.BinaryTagTypes.STRING);
        for (var name : names) {
            builder.add(StringBinaryTag.stringBinaryTag(name));
        }
        return builder.build();
    }

    /** Schématic minimal exposant une liste d'entités fixe. */
    private static Schematic schematicWith(List<CompoundBinaryTag> entities) {
        return new Schematic() {
            @Override
            public Point size() {
                return Vec.ZERO;
            }

            @Override
            public void forEachBlock(Rotation rotation, BlockConsumer consumer) {
                // aucun bloc
            }

            @Override
            public List<CompoundBinaryTag> entities() {
                return entities;
            }
        };
    }

    @Test
    void parsesSpongeStyleTopLevelTagsAndPos() {
        var e1 = CompoundBinaryTag.builder()
                .putString("id", "minecraft:armor_stand")
                .put("Pos", pos(1, 2, 3))
                .put("Tags", tags(MarkerTags.SLEEP))
                .build();
        var e2 = CompoundBinaryTag.builder()
                .put("Pos", pos(4, 5, 6))
                .put("Tags", tags(MarkerTags.SLEEP, MarkerTags.SIT))
                .build();

        var markers = SchematicMarkers.parse(schematicWith(List.of(e1, e2)));

        assertEquals(2, markers.get(MarkerTags.SLEEP).size());
        assertEquals(1, markers.get(MarkerTags.SIT).size());
        assertEquals(new Vector3d(4, 5, 6), markers.get(MarkerTags.SIT).get(0));
    }

    @Test
    void parsesStructureStyleNestedNbt() {
        var nbt = CompoundBinaryTag.builder()
                .putString("id", "minecraft:armor_stand")
                .put("Tags", tags(MarkerTags.RECEPTION))
                .build();
        var entity = CompoundBinaryTag.builder()
                .put("pos", pos(10, 64, -5))
                .put("nbt", nbt)
                .build();

        var markers = SchematicMarkers.parse(schematicWith(List.of(entity)));

        assertEquals(1, markers.get(MarkerTags.RECEPTION).size());
        assertEquals(new Vector3d(10, 64, -5), markers.get(MarkerTags.RECEPTION).get(0));
    }

    @Test
    void ignoresUntaggedEntities() {
        var untagged = CompoundBinaryTag.builder().put("Pos", pos(0, 0, 0)).build();
        assertTrue(SchematicMarkers.parse(schematicWith(List.of(untagged))).isEmpty());
    }

    @Test
    void markerSetResolvesRelativeToBaseAndFallsBack() {
        var e = CompoundBinaryTag.builder()
                .put("Pos", pos(1, 0, 1))
                .put("Tags", tags(MarkerTags.SLEEP))
                .build();
        var schematic = schematicWith(List.of(e));

        var set = MarkerSet.resolve(schematic, new Vector3d(100, 69, 200), Rotation.NONE);

        assertEquals(new Vector3d(101, 69, 201), set.first(MarkerTags.SLEEP).orElseThrow());
        // Tag absent -> repli sur la valeur par défaut
        var fallback = new Vector3d(1, 1, 1);
        assertEquals(fallback, set.firstOr(MarkerTags.RECEPTION, fallback));
    }
}
