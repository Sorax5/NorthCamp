package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlotBuilder extends MinestomBuilder<PlotData, Plot> {
    @Override
    public CompletableFuture<Void> BuildAsync(PlotData data, Plot state, InstanceContainer instance) {
        Area area = data.area();

        for (Vector3i vector3i : area.getBlocksIterator()) {
            Vector3d wordPosition = VectorMapper.toVector3d(vector3i).add(state.getPosition());
            Pos blockPos = PositionMapper.toMinestomPos(wordPosition);
            instance.setBlock(blockPos, Block.BLUE_WOOL);
        }

        /*List<Vector3i> blocks = area.getAll(VectorMapper.toVector3i(state.getPosition()));
        for (Vector3i block : blocks) {
            Vector3d wordPosition = VectorMapper.toVector3d(block);
            Pos blockPos = PositionMapper.toMinestomPos(wordPosition);
            instance.setBlock(blockPos, Block.BLUE_WOOL);
        }*/

        return CompletableFuture.completedFuture(null);
    }
}
