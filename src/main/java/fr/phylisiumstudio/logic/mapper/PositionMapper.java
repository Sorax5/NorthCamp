package fr.phylisiumstudio.logic.mapper;

import net.minestom.server.coordinate.Pos;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class PositionMapper {
    public static Pos toMinestomPos(Vector3d position) {
        return new Pos(position.x, position.y, position.z);
    }
}
