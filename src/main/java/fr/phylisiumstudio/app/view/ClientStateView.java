package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientNpc;
import lombok.Getter;
import net.minestom.server.instance.InstanceContainer;

import java.util.List;

public class ClientStateView {
    @Getter
    private final Campsite campsite;
    private final List<ClientNpc> clientsStateMachines;

    public ClientStateView(Campsite campsite, InstanceContainer instance) {
        this.campsite = campsite;
        this.clientsStateMachines = campsite.getClients().stream()
                .map(client -> new ClientNpc(client, instance, campsite))
                .toList();
    }

    public void Update(float deltaTime) {
        clientsStateMachines.forEach(ClientNpc::tick);
    }
}
