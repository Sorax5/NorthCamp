package fr.phylisiumstudio.logic.client.Action.bored;

import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Action.TimedLeafTask;
import fr.phylisiumstudio.logic.client.ClientNpc;

import java.time.Duration;

public class DoTheActivity extends TimedLeafTask {
    private Duration activityDuration;

    @Override
    protected Duration getDuration() {
        return activityDuration;
    }

    @Override
    protected boolean onStart() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        var campsite = memory.getCampsite();
        var activity = memory.getChoosenActivity();

        if (activity == null) {
            return false;
        }

        if (!activity.addClient(memory.getClient())) {
            entity.setCurrentAction(memory.getClient().getAction().toString(), "Oh, c'est plein !");
            memory.setChoosenActivity(null);
            return false;
        }

        memory.setCurrentActivity(activity);
        memory.setChoosenActivity(null);
        activityDuration = Duration.ofSeconds(activity.getDuration());

        campsite.addMoney(activity.getPrice());
        return true;
    }

    @Override
    protected void onRunning() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        var activity = memory.getCurrentActivity();

        entity.setCurrentAction(memory.getClient().getAction().toString(), "Doing " + activity.getActivityData().type());
    }

    @Override
    protected void onEnd() {
        var memory = getObject().getMemory();
        var activity = memory.getCurrentActivity();

        memory.setCurrentActivity(null);
        activity.removeClient(memory.getClient());
    }

    @Override
    protected Task<ClientNpc> copyTo(Task<ClientNpc> task) {
        return task;
    }
}
