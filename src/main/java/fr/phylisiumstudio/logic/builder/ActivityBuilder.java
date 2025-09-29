package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ActivityBuilder extends MinestomBuilder<ActivityData, Activity> {
    @Override
    public CompletableFuture<Void> BuildAsync(ActivityData data, Activity state, InstanceContainer instance) {
        Area area = data.area();

        List<Vector3i> blocks = area.getAll(VectorMapper.toVector3i(state.getPosition()));
        for (Vector3i block : blocks) {
            Vector3d wordPosition = VectorMapper.toVector3d(block);
            Pos blockPos = PositionMapper.toMinestomPos(wordPosition);
            instance.setBlock(blockPos, Block.AIR);
        }

        return CompletableFuture.completedFuture(null);
    }
}
