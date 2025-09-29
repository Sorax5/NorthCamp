package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.concurrent.CompletableFuture;

public class PlotBuilder extends MinestomBuilder<PlotData, Plot> {
    @Override
    public CompletableFuture<Void> BuildAsync(PlotData data, Plot state, InstanceContainer instance) {
        Area area = data.area();

        for (Vector3i relativePos : area.getBlocksIterator()) {
            Vector3d wordPosition = state.getPosition().add(relativePos.x, relativePos.y, relativePos.z);
            Pos blockPos = PositionMapper.toMinestomPos(wordPosition);
            instance.setBlock(blockPos, Block.STONE);
        }

        return CompletableFuture.completedFuture(null);
    }
}
