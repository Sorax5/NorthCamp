package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientEntity;

public class UpdateClientState extends LeafTask<ClientEntity> {
    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        var client = memory.getClient();

        double r = Math.random();
        Client.ClientState newState;
        if (r < 0.6) {
            newState = Client.ClientState.BORED; // 60%
        } else if (r < 0.85) {
            newState = Client.ClientState.CHILL; // 25%
        } else {
            newState = Client.ClientState.SLEEPY; // 15%
        }

        var current = client.getAction();
        if (current != null && current == newState) {
            for (var state : Client.ClientState.values()) {
                if (state != current) {
                    newState = state;
                    break;
                }
            }
        }

        client.setAction(newState);

        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
