package fr.phylisiumstudio.logic.client.Action.chill;

import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Action.TimedLeafTask;
import fr.phylisiumstudio.logic.client.ClientEntity;

import java.time.Duration;

public class JustChillHome extends TimedLeafTask {

    @Override
    protected Duration getDuration() {
        return Duration.ofSeconds(10);
    }

    @Override
    protected boolean onStart() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        entity.setCurrentAction("Just chilling at home");
        return true;
    }

    @Override
    protected void onRunning() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        entity.setCurrentAction("Just chilling at home, time left: " + getTimeLeft().toSeconds() + "s");
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
