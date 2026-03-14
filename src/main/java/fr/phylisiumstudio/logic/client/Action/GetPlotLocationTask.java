package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.mapper.PositionMapper;

public class GetPlotLocationTask extends LeafTask<ClientEntity> {
    @Override
    public Status execute() {
        var memory = getObject().getMemory();

        if (memory.getTargetPosition() != null) {
            return Status.SUCCEEDED;
        }

        var plotLocation = memory.getClient().getPlot().getPosition();
        if (plotLocation == null) {
            return Status.FAILED;
        }

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
