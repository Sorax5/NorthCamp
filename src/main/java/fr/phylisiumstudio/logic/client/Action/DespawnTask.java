package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientLifecycle;

/**
 * Fait disparaître le NPC une fois arrivé à la sortie : marque le client comme
 * parti puis retire l'entité. La boucle de gameplay nettoie ensuite le domaine.
 */
public class DespawnTask extends LeafTask<ClientEntity> {

    @Override
    public Status execute() {
        var entity = getObject();
        entity.getMemory().getClient().setLifecycle(ClientLifecycle.GONE);
        entity.remove();
        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
