package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.logic.area.AreaBlockIterator;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;

import java.util.concurrent.CompletableFuture;

public class PlotBuilder extends MinestomBuilder<PlotData, Plot> {
    private final SchematicFactory schematicFactory;

    public PlotBuilder(SchematicFactory schematicFactory) {
        this.schematicFactory = schematicFactory;
    }

    @Override
    public CompletableFuture<Void> BuildAsync(PlotData data, Plot state, InstanceContainer instance) {
        var future = new CompletableFuture<Void>();
        try {
            var schematic = schematicFactory.getSchematic(data.schem());

            var batch = schematic.createBatch(Rotation.NONE);

            batch.apply(instance, PositionMapper.toMinestomPos(state.getPosition()), blockBatch -> {
                var area = data.area();

                var areaBlockIterator = new AreaBlockIterator(area);
                while (areaBlockIterator.hasNext()) {
                    var vector3i = areaBlockIterator.next();
                    if (!area.isGroundBlock(vector3i) || !area.isWallBlock(vector3i)) {
                        continue;
                    }

                    var wordPosition = VectorMapper.toVector3d(vector3i).add(state.getPosition());
                    var blockPos = PositionMapper.toMinestomPos(wordPosition);
                    blockBatch.setBlock(blockPos, Block.WHITE_WOOL);
                }

                future.complete(null);
            });

            return future;
        }
        catch (Exception e) {
            System.err.println("Failed to build plot: " + e.getMessage());
            future.completeExceptionally(e);
            return future;
        }
    }
}
