package fr.phylisiumstudio.logic.client;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import lombok.Getter;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class ClientNpc {
    private static final Logger logger = LoggerFactory.getLogger(ClientNpc.class);

    private final BehaviorTree<ClientNpc> behaviorTree;
    private final ClientMemory memory;

    public ClientNpc(Client client, InstanceContainer instance, Campsite campsite) {
        this.memory = new ClientMemory(instance, client, campsite);
        this.behaviorTree = new BehaviorTree<>(new ClientRootNode(), this);

        spawnNpc();
    }

    public void tick() {
        try {
            behaviorTree.step();
        } catch (Exception e) {
            logger.error("Erreur dans le behavior tree du client {}", memory.getClient().getUniqueID(), e);
        }
    }

    public void spawnNpc() {
        try {
            var client = memory.getClient();
            if (client == null || client.getPlot() == null) {
                logger.warn("Impossible de spawn le NPC: client ou plot null");
                return;
            }

            var spawnLocation = client.getPlot().getPosition();
            var spawnPos = PositionMapper.toMinestomPos(spawnLocation);
            var instance = memory.getInstance();

            var entity = new ClientEntity();
            entity.setInstance(instance, spawnPos);

            memory.setPlayerEntity(entity);
            logger.debug("NPC spawné pour le client {} en {}", client.getUniqueID(), spawnPos);
        } catch (Exception e) {
            logger.error("Erreur lors du spawn du NPC", e);
        }
    }
}
