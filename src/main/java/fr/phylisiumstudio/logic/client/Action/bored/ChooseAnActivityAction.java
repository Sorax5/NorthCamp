package fr.phylisiumstudio.logic.client.Action.bored;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.mapper.PositionMapper;

public class ChooseAnActivityAction extends LeafTask<ClientEntity> {
    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        var campsite = memory.getCampsite();

        if (memory.getChoosenActivity() != null || memory.getCurrentActivity() != null) {
            return Status.SUCCEEDED;
        }

        if (campsite.getActivities().isEmpty()) {
            return Status.FAILED;
        }

        var randomIndex = (int) (Math.random() * campsite.getActivities().size());
        var chosenActivity = campsite.getActivities().get(randomIndex);

        memory.setChoosenActivity(chosenActivity);
        memory.setTargetPosition(PositionMapper.toMinestomPos(chosenActivity.getPosition()));

        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
