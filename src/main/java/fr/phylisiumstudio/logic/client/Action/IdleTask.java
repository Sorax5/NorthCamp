package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;

import java.time.Duration;

/**
 * Attente sur place pendant une durée donnée (patiente à l'accueil, etc.).
 */
public class IdleTask extends TimedLeafTask {
    private final String label;
    private final Duration duration;

    public IdleTask(String label, Duration duration) {
        this.label = label;
        this.duration = duration;
    }

    @Override
    protected Duration getDuration() {
        return duration;
    }

    @Override
    protected void onRunning() {
        getObject().getMemory().getPlayerEntity()
                .setCurrentAction(label + " (" + getTimeLeft().toSeconds() + "s)");
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
