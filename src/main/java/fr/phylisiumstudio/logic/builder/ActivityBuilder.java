package fr.phylisiumstudio.logic.builder;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;

import java.util.concurrent.CompletableFuture;

@Singleton
public class ActivityBuilder extends MinestomBuilder<ActivityData, Activity> {
    @Override
    public CompletableFuture<Void> BuildAsync(ActivityData data, Activity state, InstanceContainer instance) {
        var area = data.area();
        var position = state.getPosition();

        var min = area.getMinCorner();
        var max = area.getMaxCorner();

        for (var x = min.x; x <= max.x; x++) {
            for (var z = min.z; z <= max.z; z++) {
                if (x == min.x || x == max.x || z == min.z || z == max.z) {
                    var vector = new Vector3d(position).add(x, -1, z);
                    instance.setBlock(PositionMapper.toMinestomPos(vector), Block.STONE);
                }
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
