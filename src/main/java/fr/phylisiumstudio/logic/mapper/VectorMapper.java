package fr.phylisiumstudio.logic.mapper;

import org.joml.Vector3d;
import org.joml.Vector3i;

public class VectorMapper {
    public static Vector3d toVector3d(Vector3i vector) {
        return new Vector3d(vector.x, vector.y, vector.z);
    }

    public static Vector3i toVector3i(Vector3d vector) {
        return new Vector3i((int) vector.x, (int) vector.y, (int) vector.z);
    }
}
