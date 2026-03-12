package fr.phylisiumstudio.logic.builder;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.batch.BatchOption;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;

import java.util.concurrent.CompletableFuture;

@Singleton
public class ActivityBuilder extends MinestomBuilder<ActivityData, Activity> {
    @Override
    public CompletableFuture<Void> BuildAsync(ActivityData data, Activity state, InstanceContainer instance) {
        var future = new CompletableFuture<Void>();
        var area = data.area();
        var position = state.getPosition();

        var min = area.getMinCorner();
        var max = area.getMaxCorner();

        var blockbatch = new AbsoluteBlockBatch(new BatchOption());

        for (var x = min.x; x <= max.x; x++) {
            for (var z = min.z; z <= max.z; z++) {
                if (x == min.x || x == max.x || z == min.z || z == max.z) {
                    var vector = new Vector3d(position).add(x, -1, z);
                    blockbatch.setBlock(PositionMapper.toMinestomPos(vector), Block.STONE);
                    instance.setBlock(PositionMapper.toMinestomPos(vector), Block.STONE);
                }
            }
        }

        blockbatch.apply(instance,absoluteBlockBatch -> {
           future.complete(null);
        });

        return future;
    }
}
