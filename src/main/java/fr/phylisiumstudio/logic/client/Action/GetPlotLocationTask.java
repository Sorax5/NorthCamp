package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.marker.MarkerRegistry;
import fr.phylisiumstudio.logic.marker.MarkerTags;

public class GetPlotLocationTask extends LeafTask<ClientEntity> {
    @Override
    public Status execute() {
        var memory = getObject().getMemory();

        if (memory.getTargetPosition() != null) {
            return Status.SUCCEEDED;
        }

        var plot = memory.getClient().getPlot();
        if (plot == null || plot.getPosition() == null) {
            return Status.FAILED;
        }

        // Cible = marqueur de sommeil du schématic si présent, sinon la position du plot.
        var plotLocation = MarkerRegistry.instance().get(plot.getUniqueID())
                .firstOr(MarkerTags.SLEEP, plot.getPosition());

        var plotPos = PositionMapper.toMinestomPos(plotLocation);
        memory.setTargetPosition(plotPos);

        memory.getPlayerEntity().setCurrentAction("Heading to plot location");

        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
