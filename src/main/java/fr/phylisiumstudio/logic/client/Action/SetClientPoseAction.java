package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import net.minestom.server.entity.EntityPose;

public class SetClientPoseAction extends LeafTask<ClientEntity> {
    private final EntityPose pose;

    public SetClientPoseAction(EntityPose pose) {
        this.pose = pose;
    }

    @Override
    public Status execute() {
        var entity = getObject();
        if (entity == null) {
            return Status.FAILED;
        }

        entity.setPose(pose);

        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
