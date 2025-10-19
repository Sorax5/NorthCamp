package fr.phylisiumstudio.logic.builder;

import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.area.Area;
import fr.phylisiumstudio.logic.area.AreaBlockIterator;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import net.hollowcube.schem.Rotation;
import net.hollowcube.schem.Schematic;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.RelativeBlockBatch;
import net.minestom.server.instance.block.Block;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PlotBuilder extends MinestomBuilder<PlotData, Plot> {
    private final SchematicFactory schematicFactory;

    private final Map<PlotType, String> schematicMap = Map.of(
            PlotType.CAMPSITE, "camp_1.schem",
            PlotType.CARAVAN, "car.schem"
    );

    public PlotBuilder(SchematicFactory schematicFactory) {
        this.schematicFactory = schematicFactory;
    }

    @Override
    public CompletableFuture<Void> BuildAsync(PlotData data, Plot state, InstanceContainer instance) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            String schematicName = schematicMap.get(data.type());
            Schematic schematic = schematicFactory.getSchematic(schematicName);

            RelativeBlockBatch batch = schematic.build(Rotation.NONE, true);
            batch.apply(instance, PositionMapper.toMinestomPos(state.getPosition()), () -> {
                Area area = data.area();

                AreaBlockIterator areaBlockIterator = new AreaBlockIterator(area);
                while (areaBlockIterator.hasNext()) {
                    Vector3i vector3i = areaBlockIterator.next();
                    if (!area.isGroundBlock(vector3i) || !area.isWallBlock(vector3i)) {
                        continue;
                    }

                    Vector3d wordPosition = VectorMapper.toVector3d(vector3i).add(state.getPosition());
                    Pos blockPos = PositionMapper.toMinestomPos(wordPosition);
                    instance.setBlock(blockPos, Block.WHITE_WOOL);
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
