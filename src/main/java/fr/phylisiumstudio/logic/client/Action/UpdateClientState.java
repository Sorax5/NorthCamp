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

        var states = Client.ClientState.values();
        var randomIndex = (int) (Math.random() * states.length);
        var newState = states[randomIndex];
        client.setAction(newState);

        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
