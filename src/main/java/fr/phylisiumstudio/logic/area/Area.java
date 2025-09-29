package fr.phylisiumstudio.logic.area;

import fr.phylisiumstudio.logic.mapper.VectorMapper;
import lombok.Data;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

@Data
public class Area {
    private final Vector3d firstCorner;
    private final Vector3d secondCorner;

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
}
