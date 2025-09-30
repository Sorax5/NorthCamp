package fr.phylisiumstudio.logic.area;

import fr.phylisiumstudio.logic.mapper.VectorMapper;
import lombok.Getter;
import org.joml.Vector3i;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Getter
public class AreaBlockIterator implements Iterator<Vector3i> {
    private final Vector3i min;
    private final Vector3i max;
    private Vector3i current;

    public AreaBlockIterator(Area area) {
        this.min = VectorMapper.toVector3i(area.getMinCorner());
        this.max = VectorMapper.toVector3i(area.getMaxCorner());
        this.current = new Vector3i(min);
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    @Override
    public Vector3i next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        Vector3i result = new Vector3i(current);

        // Avance x, puis y, puis z, sans imbrication
        if (current.x < max.x) {
            current.x++;
            return result;
        }
        current.x = min.x;

        if (current.y < max.y) {
            current.y++;
            return result;
        }
        current.y = min.y;

        if (current.z < max.z) {
            current.z++;
            return result;
        }

        current = null;
        return result;
    }
}
