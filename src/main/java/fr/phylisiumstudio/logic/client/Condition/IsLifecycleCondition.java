package fr.phylisiumstudio.logic.client.Condition;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientLifecycle;

/**
 * Réussit si le client est dans l'étape de séjour (macro) attendue.
 */
public class IsLifecycleCondition extends LeafTask<ClientEntity> {
    private final ClientLifecycle expected;

    public IsLifecycleCondition(ClientLifecycle expected) {
        this.expected = expected;
    }

    @Override
    public Status execute() {
        return getObject().getMemory().getClient().getLifecycle() == expected
                ? Status.SUCCEEDED
                : Status.FAILED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
