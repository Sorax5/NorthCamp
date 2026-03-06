package fr.phylisiumstudio.logic.client;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import lombok.Getter;
import net.minestom.server.instance.InstanceContainer;

@Getter
public class ClientNpc {
    private final BehaviorTree<ClientNpc> behaviorTree;
    private final ClientMemory memory;

    public ClientNpc(Client client, InstanceContainer instance, Campsite campsite) {
        this.memory = new ClientMemory(instance, client, campsite);
        this.behaviorTree = new BehaviorTree<>(new ClientRootNode(), this);

        spawnNpc();
    }

    public void tick() {
        behaviorTree.step();
    }

    public void spawnNpc() {
        var spawnLocation = memory.getClient().getPlot().getPosition();
        var spawnPos = PositionMapper.toMinestomPos(spawnLocation);

        var instance = memory.getInstance();

        var entity = new ClientEntity();
        entity.setInstance(instance, spawnPos);

        memory.setPlayerEntity(entity);
    }
}
