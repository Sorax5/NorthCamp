package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.concurrent.CompletableFuture;

public class ActivityBuilder implements Builder<ActivityData, Activity, InstanceContainer> {
    @Override
    public CompletableFuture<Void> Build(ActivityData data, Activity state, InstanceContainer instance) {
        Area area = data.area();

        for (Vector3i relativePos : area.getBlocksIterator()) {
            Vector3d wordPosition = state.getPosition().add(relativePos.x, relativePos.y, relativePos.z);
            Pos blockPos = PositionMapper.toMinestomPos(wordPosition);
            instance.setBlock(blockPos, Block.STONE);
        }

        return CompletableFuture.completedFuture(null);
    }
}
