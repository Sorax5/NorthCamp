package fr.phylisiumstudio.logic.client.Action.bored;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.marker.MarkerRegistry;
import fr.phylisiumstudio.logic.marker.MarkerTags;

public class ChooseAnActivityAction extends LeafTask<ClientEntity> {
    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        var campsite = memory.getCampsite();

        if (memory.getChoosenActivity() != null || memory.getCurrentActivity() != null) {
            return Status.SUCCEEDED;
        }

        // Seules les activités opérationnelles sont choisissables.
        var available = campsite.getActivities().stream()
                .filter(Activity::isOperational)
                .toList();
        if (available.isEmpty()) {
            return Status.FAILED;
        }

        var randomIndex = (int) (Math.random() * available.size());
        var chosenActivity = available.get(randomIndex);

        // Cible = marqueur d'activité du schématic si présent, sinon la position de l'activité.
        var target = MarkerRegistry.instance().get(chosenActivity.getUniqueID())
                .firstOr(MarkerTags.ACTIVITY_TARGET, chosenActivity.getPosition());

        memory.setChoosenActivity(chosenActivity);
        memory.setTargetPosition(PositionMapper.toMinestomPos(target));

        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
