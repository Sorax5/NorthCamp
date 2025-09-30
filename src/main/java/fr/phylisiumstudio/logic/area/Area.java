package fr.phylisiumstudio.logic.area;

import fr.phylisiumstudio.logic.mapper.VectorMapper;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Data
public class Area {
    private final Vector3d firstCorner;
    private final Vector3d secondCorner;

    private final Predicate<Vector3i> IS_GROUND_BLOCK = (vec) -> vec.y == getMinCorner().y;

    private final Predicate<Vector3i> IS_WALL_BLOCK = (vec) ->
        (vec.x == getMinCorner().x || vec.x == getMaxCorner().x ||
         vec.z == getMinCorner().z || vec.z == getMaxCorner().z) &&
        (vec.y >= getMinCorner().y && vec.y <= getMaxCorner().y);

    public Iterable<Vector3i> getBlocksIterator() {
        return () -> new AreaBlockIterator(this);
    }

    public double getVolume() {
        Vector3d size = new Vector3d(
                Math.abs(secondCorner.x - firstCorner.x) + 1,
                Math.abs(secondCorner.y - firstCorner.y) + 1,
                Math.abs(secondCorner.z - firstCorner.z) + 1
        );
        return size.x * size.y * size.z;
    }

    public Vector2d getMaxVerticalFace() {
        return new Vector2d(
                Math.abs(secondCorner.x - firstCorner.x) + 1,
                Math.abs(secondCorner.z - firstCorner.z) + 1
        );
    }

    public Vector3d getMinCorner() {
        return new Vector3d(
                Math.min(firstCorner.x, secondCorner.x),
                Math.min(firstCorner.y, secondCorner.y),
                Math.min(firstCorner.z, secondCorner.z)
        );
    }

    public Vector3d getMaxCorner() {
        return new Vector3d(
                Math.max(firstCorner.x, secondCorner.x),
                Math.max(firstCorner.y, secondCorner.y),
                Math.max(firstCorner.z, secondCorner.z)
        );
    }

    public Vector3d getSize() {
        Vector3d min = getMinCorner();
        Vector3d max = getMaxCorner();
        return new Vector3d(
                Math.abs(max.x - min.x) + 1,
                Math.abs(max.y - min.y) + 1,
                Math.abs(max.z - min.z) + 1
        );
    }

    public List<Vector3i> getAll(Vector3i base) {
        Vector3d min = getMinCorner();
        Vector3d max = getMaxCorner();

        List<Vector3i> blocks = new ArrayList<>();

        Vector3i minInt = VectorMapper.toVector3i(min);
        Vector3i maxInt = VectorMapper.toVector3i(max);

        for (int x = minInt.x; x <= maxInt.x; x++) {
            for (int y = minInt.y; y <= maxInt.y; y++) {
                for (int z = minInt.z; z <= maxInt.z; z++) {
                    blocks.add(new Vector3i(x + base.x, y + base.y, z + base.z));
                }
            }
        }

        return blocks;
    }

    public boolean isWallBlock(Vector3i position) {
        return IS_WALL_BLOCK.test(position);
    }

    public boolean isGroundBlock(Vector3i position) {
        return IS_GROUND_BLOCK.test(position);
    }
}
