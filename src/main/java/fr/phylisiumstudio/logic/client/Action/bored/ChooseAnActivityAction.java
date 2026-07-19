package fr.phylisiumstudio.logic.client.Action.bored;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityChooser;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.marker.MarkerRegistry;
import fr.phylisiumstudio.logic.marker.MarkerTags;

import java.util.Random;

public class ChooseAnActivityAction extends LeafTask<ClientEntity> {
    private static final Random RANDOM = new Random();

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

        // Choix pondéré par l'heure : certaines activités sont préférées le jour,
        // d'autres la nuit (l'heure vient du cycle interne appliqué à l'instance).
        boolean isDay = isDay(memory.getInstance().getTime());
        var chosen = ActivityChooser.choose(available, isDay, RANDOM);
        if (chosen.isEmpty()) {
            return Status.FAILED;
        }
        var chosenActivity = chosen.get();

        // Cible = marqueur d'activité du schématic si présent, sinon la position de l'activité.
        var target = MarkerRegistry.instance().get(chosenActivity.getUniqueID())
                .firstOr(MarkerTags.ACTIVITY_TARGET, chosenActivity.getPosition());

        memory.setChoosenActivity(chosenActivity);
        memory.setTargetPosition(PositionMapper.toMinestomPos(target));

        return Status.SUCCEEDED;
    }

    /** Jour si le tick de temps Minecraft est dans la première moitié (0–12000). */
    private static boolean isDay(long minecraftTime) {
        long t = ((minecraftTime % 24000) + 24000) % 24000;
        return t < 12000;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
