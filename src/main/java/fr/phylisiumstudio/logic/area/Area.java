package fr.phylisiumstudio.logic.area;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public record Area(@JsonProperty Vector3d firstCorner, @JsonProperty Vector3d secondCorner) {
    @JsonCreator
    public Area(@JsonProperty("firstCorner") Vector3d firstCorner,
                @JsonProperty("secondCorner") Vector3d secondCorner) {
        Objects.requireNonNull(firstCorner, "firstCorner must not be null");
        Objects.requireNonNull(secondCorner, "secondCorner must not be null");
        this.firstCorner = new Vector3d(firstCorner);
        this.secondCorner = new Vector3d(secondCorner);
    }

    public Area(Vector3i a, Vector3i b) {
        Objects.requireNonNull(a, "a must not be null");
        Objects.requireNonNull(b, "b must not be null");
        this(new Vector3d(a.x, a.y, a.z), new Vector3d(b.x, b.y, b.z));
    }

    public static Area fromInts(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Area(new Vector3d(x1, y1, z1), new Vector3d(x2, y2, z2));
    }

    @Override
    public Vector3d firstCorner() {
        return new Vector3d(firstCorner);
    }

    @Override
    public Vector3d secondCorner() {
        return new Vector3d(secondCorner);
    }

    public Iterable<Vector3i> getBlocksIterator() {
        return () -> new AreaBlockIterator(this);
    }

    public double getVolume() {
        var size = new Vector3d(
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
        var min = getMinCorner();
        var max = getMaxCorner();
        return new Vector3d(
                Math.abs(max.x - min.x) + 1,
                Math.abs(max.y - min.y) + 1,
                Math.abs(max.z - min.z) + 1
        );
    }

    public List<Vector3i> getAll() {
        return getAll(new Vector3i(0, 0, 0));
    }

    public List<Vector3i> getAll(Vector3i base) {
        Objects.requireNonNull(base, "base must not be null");
        var min = getMinCorner();
        var max = getMaxCorner();

        var minInt = VectorMapper.toVector3i(min);
        var maxInt = VectorMapper.toVector3i(max);

        var lx = maxInt.x - minInt.x + 1L;
        var ly = maxInt.y - minInt.y + 1L;
        var lz = maxInt.z - minInt.z + 1L;
        var total = lx * ly * lz;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalStateException("Area too large to list all blocks: " + total);
        }

        var blocks = new ArrayList<Vector3i>((int) total);

        for (var x = minInt.x; x <= maxInt.x; x++) {
            for (var y = minInt.y; y <= maxInt.y; y++) {
                for (var z = minInt.z; z <= maxInt.z; z++) {
                    blocks.add(new Vector3i(x + base.x, y + base.y, z + base.z));
                }
            }
        }

        return blocks;
    }

    public boolean isWallBlock(Vector3i position) {
        Objects.requireNonNull(position, "position must not be null");
        var min = VectorMapper.toVector3i(getMinCorner());
        var max = VectorMapper.toVector3i(getMaxCorner());
        return ((position.x == min.x || position.x == max.x || position.z == min.z || position.z == max.z)
                && (position.y >= min.y && position.y <= max.y));
    }

    public boolean isGroundBlock(Vector3i position) {
        Objects.requireNonNull(position, "position must not be null");
        var min = VectorMapper.toVector3i(getMinCorner());
        return position.y == min.y;
    }

    public boolean contains(Vector3i position) {
        Objects.requireNonNull(position, "position must not be null");
        var min = VectorMapper.toVector3i(getMinCorner());
        var max = VectorMapper.toVector3i(getMaxCorner());
        return position.x >= min.x && position.x <= max.x
                && position.y >= min.y && position.y <= max.y
                && position.z >= min.z && position.z <= max.z;
    }

    public boolean contains(Area other) {
        Objects.requireNonNull(other, "other must not be null");
        var aMin = VectorMapper.toVector3i(getMinCorner());
        var aMax = VectorMapper.toVector3i(getMaxCorner());
        var bMin = VectorMapper.toVector3i(other.getMinCorner());
        var bMax = VectorMapper.toVector3i(other.getMaxCorner());
        return bMin.x >= aMin.x && bMax.x <= aMax.x
                && bMin.y >= aMin.y && bMax.y <= aMax.y
                && bMin.z >= aMin.z && bMax.z <= aMax.z;
    }

    public boolean intersects(Area other) {
        Objects.requireNonNull(other, "other must not be null");
        var aMin = VectorMapper.toVector3i(getMinCorner());
        var aMax = VectorMapper.toVector3i(getMaxCorner());
        var bMin = VectorMapper.toVector3i(other.getMinCorner());
        var bMax = VectorMapper.toVector3i(other.getMaxCorner());

        return (aMin.x <= bMax.x && aMax.x >= bMin.x)
                && (aMin.y <= bMax.y && aMax.y >= bMin.y)
                && (aMin.z <= bMax.z && aMax.z >= bMin.z);
    }

    public Vector3d center() {
        return new Vector3d(
                (firstCorner.x + secondCorner.x) / 2.0,
                (firstCorner.y + secondCorner.y) / 2.0,
                (firstCorner.z + secondCorner.z) / 2.0
        );
    }

    public Area translate(Vector3i offset) {
        Objects.requireNonNull(offset, "offset must not be null");
        return new Area(
                new Vector3d(firstCorner.x + offset.x, firstCorner.y + offset.y, firstCorner.z + offset.z),
                new Vector3d(secondCorner.x + offset.x, secondCorner.y + offset.y, secondCorner.z + offset.z)
        );
    }

    public Area expand(int dx, int dy, int dz) {
        var min = getMinCorner();
        var max = getMaxCorner();
        return new Area(
                new Vector3d(min.x - dx, min.y - dy, min.z - dz),
                new Vector3d(max.x + dx, max.y + dy, max.z + dz)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Area area)) return false;
        return Double.doubleToLongBits(firstCorner.x) == Double.doubleToLongBits(area.firstCorner.x)
                && Double.doubleToLongBits(firstCorner.y) == Double.doubleToLongBits(area.firstCorner.y)
                && Double.doubleToLongBits(firstCorner.z) == Double.doubleToLongBits(area.firstCorner.z)
                && Double.doubleToLongBits(secondCorner.x) == Double.doubleToLongBits(area.secondCorner.x)
                && Double.doubleToLongBits(secondCorner.y) == Double.doubleToLongBits(area.secondCorner.y)
                && Double.doubleToLongBits(secondCorner.z) == Double.doubleToLongBits(area.secondCorner.z);
    }

    @Override
    public int hashCode() {
        long h1 = Double.doubleToLongBits(firstCorner.x);
        long h2 = Double.doubleToLongBits(firstCorner.y);
        long h3 = Double.doubleToLongBits(firstCorner.z);
        long h4 = Double.doubleToLongBits(secondCorner.x);
        long h5 = Double.doubleToLongBits(secondCorner.y);
        long h6 = Double.doubleToLongBits(secondCorner.z);
        int result = 17;
        result = 31 * result + Long.hashCode(h1);
        result = 31 * result + Long.hashCode(h2);
        result = 31 * result + Long.hashCode(h3);
        result = 31 * result + Long.hashCode(h4);
        result = 31 * result + Long.hashCode(h5);
        result = 31 * result + Long.hashCode(h6);
        return result;
    }

    @Override
    public String toString() {
        return "Area{" +
                "min=" + getMinCorner() +
                ", max=" + getMaxCorner() +
                '}';
    }
}
