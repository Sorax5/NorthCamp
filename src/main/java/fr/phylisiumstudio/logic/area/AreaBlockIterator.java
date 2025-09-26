package fr.phylisiumstudio.logic.area;

import org.joml.Vector3i;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class AreaBlockIterator implements Iterator<Vector3i> {
    private final Vector3i min;
    private final Vector3i max;
    private Vector3i current;

    public AreaBlockIterator(Area area) {
        this.min = new Vector3i(
                (int) Math.floor(area.getMinCorner().x),
                (int) Math.floor(area.getMinCorner().y),
                (int) Math.floor(area.getMinCorner().z)
        );
        this.max = new Vector3i(
                (int) Math.floor(area.getMaxCorner().x),
                (int) Math.floor(area.getMaxCorner().y),
                (int) Math.floor(area.getMaxCorner().z)
        );
        this.current = new Vector3i(min);
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    @Override
    public Vector3i next() {
        if (!hasNext()) throw new NoSuchElementException();
        Vector3i result = new Vector3i(current);

        if (current.x < max.x) {
            current.x++;
        } else {
            current.x = min.x;
            if (current.y < max.y) {
                current.y++;
            } else {
                current.y = min.y;
                if (current.z < max.z) {
                    current.z++;
                } else {
                    current = null;
                }
            }
        }
        return result;
    }
}
