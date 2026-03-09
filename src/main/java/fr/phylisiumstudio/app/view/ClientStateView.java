package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientMemory;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import lombok.Getter;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ClientStateView {
    private static final Logger logger = LoggerFactory.getLogger(ClientStateView.class);

    @Getter
    private final Campsite campsite;
    private final List<ClientEntity> clientEntities;

    public ClientStateView(Campsite campsite, InstanceContainer instance) {
        this.campsite = campsite;
        this.clientEntities = new ArrayList<>();

        for (var client : campsite.getClients()) {
            var memory = new ClientMemory(instance, client, campsite);
            var entity = spawnNpc(memory);
            clientEntities.add(entity);
        }
    }

    private ClientEntity spawnNpc(ClientMemory memory) {
        var spawnLocation = memory.client.getPlot().getPosition();
        var spawnPos = PositionMapper.toMinestomPos(spawnLocation);
        var instance = memory.getInstance();

        var entity = new ClientEntity(memory);
        entity.setInstance(instance, spawnPos);

        memory.setPlayerEntity(entity);
        return entity;
    }
}
