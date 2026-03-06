package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.concurrent.CompletableFuture;

public class ActivityBuilder extends MinestomBuilder<ActivityData, Activity> {
    @Override
    public CompletableFuture<Void> BuildAsync(ActivityData data, Activity state, InstanceContainer instance) {
        var area = data.area();
        var position = state.getPosition();

        var min = area.getMinCorner();
        var max = area.getMaxCorner();

        int startX = (int) min.x;
        int endX = (int) max.x;
        int startZ = (int) min.z;
        int endZ = (int) max.z;

        for (var x = startX; x <= endX; x++) {
            for (var z = startZ; z <= endZ; z++) {
                if (x == startX || x == endX || z == startZ || z == endZ) {
                    var pos = new Pos(position.x + x, position.y - 1, position.z + z);
                    instance.setBlock(pos, Block.STONE);
                }
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
