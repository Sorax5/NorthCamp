package fr.phylisiumstudio.logic.client.Action.sleep;

import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Action.TimedLeafTask;
import fr.phylisiumstudio.logic.client.ClientNpc;

import java.time.Duration;

public class SleepTask extends TimedLeafTask {

    @Override
    protected Duration getDuration() {
        return Duration.ofSeconds(10);
    }

    @Override
    protected boolean onStart() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        entity.setCurrentAction(memory.getClient().getAction().toString(), "Sleeping");
        entity.setSleeping();
        return true;
    }

    @Override
    protected void onRunning() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        entity.setCurrentAction(memory.getClient().getAction().toString(), "Sleeping");
    }

    @Override
    protected void onEnd() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        entity.setStanding();
    }

    @Override
    protected Task<ClientNpc> copyTo(Task<ClientNpc> task) {
        return task;
    }
}
