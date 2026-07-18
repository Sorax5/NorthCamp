package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientMemory;
import net.minestom.server.coordinate.Pos;

import java.util.function.Function;

/**
 * Fixe la cible de déplacement du NPC à partir d'une position de la mémoire
 * (accueil, sortie…). Échoue si la position n'est pas définie.
 */
public class SetTargetAction extends LeafTask<ClientEntity> {

    private final Function<ClientMemory, Pos> targetSupplier;

    public SetTargetAction(Function<ClientMemory, Pos> targetSupplier) {
        this.targetSupplier = targetSupplier;
    }

    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        if (memory.getTargetPosition() != null) {
            return Status.SUCCEEDED;
        }
        var target = targetSupplier.apply(memory);
        if (target == null) {
            return Status.FAILED;
        }
        memory.setTargetPosition(target);
        return Status.SUCCEEDED;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
