package fr.phylisiumstudio.logic.client.Condition;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientEntity;

public class IsClientStateCondition extends LeafTask<ClientEntity> {
    public final Client.ClientState conditionState;

    public IsClientStateCondition(Client.ClientState conditionState) {
        this.conditionState = conditionState;
    }

    @Override
    public Status execute() {
        var currentState = getObject().getMemory().getClient().getAction();
        if (currentState == null) {
            return Status.FAILED;
        }

        if (currentState == conditionState) {
            return Status.SUCCEEDED;
        } else {
            return Status.FAILED;
        }
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
