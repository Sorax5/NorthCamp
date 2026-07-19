package fr.phylisiumstudio.logic.client.Action.bored;

import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Action.TimedLeafTask;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.effect.Effects;

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

        var client = memory.getClient();

        if (!activity.addClient(client)) {
            entity.setCurrentAction("Oh, c'est plein !");
            // Activité pleine : petite déception.
            SatisfactionService.applyActivityUnavailable(client);
            memory.setChoosenActivity(null);
            return false;
        }

        if (!activity.consumeSupply()) {
            // Rupture de stock : l'activité ne peut pas servir ce client.
            entity.setCurrentAction("En rupture de stock");
            activity.removeClient(client);
            SatisfactionService.applyActivityUnavailable(client);
            memory.setChoosenActivity(null);
            return false;
        }

        memory.setCurrentActivity(activity);
        memory.setChoosenActivity(null);
        activityDuration = Duration.ofSeconds(activity.getDuration());

        // Revenu plafonné au budget du client (plus de dépense infinie), et
        // profiter d'une activité remonte sa satisfaction.
        double earned = EconomyService.collectActivityIncome(campsite, client, activity.getPrice());
        SatisfactionService.applyActivityEnjoyed(client, activity.getType());
        Effects.moneyPopup(entity.getInstance(), entity.getPosition(), earned);
        return true;
    }

    @Override
    protected void onRunning() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        var activity = memory.getCurrentActivity();

        entity.setCurrentAction("Doing " + activity.getType() + " (" + getTimeLeft().toSeconds() + "s left)");
    }

    @Override
    protected void onEnd() {
        var memory = getObject().getMemory();
        var entity = getObject();
        var activity = memory.getCurrentActivity();

        memory.setCurrentActivity(null);
        activity.removeClient(memory.getClient());

        Effects.activityDone(entity.getInstance(), entity.getPosition());
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
