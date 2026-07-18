package fr.phylisiumstudio.logic.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.area.AreaBlockIterator;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import fr.phylisiumstudio.logic.marker.MarkerRegistry;
import fr.phylisiumstudio.logic.marker.MarkerSet;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import net.hollowcube.schem.util.Rotation;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@Singleton
public class PlotBuilder extends MinestomBuilder<PlotData, Plot> {
    private final SchematicFactory schematicFactory;
    private final MarkerRegistry markerRegistry;
    private final Logger logger = LoggerFactory.getLogger(PlotBuilder.class);

    @Inject
    public PlotBuilder(SchematicFactory schematicFactory, MarkerRegistry markerRegistry) {
        this.schematicFactory = schematicFactory;
        this.markerRegistry = markerRegistry;
    }

    @Override
    public CompletableFuture<Void> BuildAsync(PlotData data, Plot state, InstanceContainer instance) {
        var future = new CompletableFuture<Void>();
        var schematic = schematicFactory.getSchematic(data.schem());

        // Rotation de base conservée (Rotation.NONE) pour les blocs comme pour les marqueurs.
        var markers = MarkerSet.resolve(schematic, state.getPosition(), Rotation.NONE);
        markerRegistry.register(state.getUniqueID(), markers);

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
}
