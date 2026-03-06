package fr.phylisiumstudio.logic.client.Condition;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientNpc;

public class IsClientStateCondition extends LeafTask<ClientNpc> {
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
    protected Task<ClientNpc> copyTo(Task<ClientNpc> task) {
        return task;
    }
}
