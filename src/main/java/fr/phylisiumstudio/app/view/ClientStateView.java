package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientNpc;
import lombok.Getter;
import net.minestom.server.instance.InstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClientStateView {
    private static final Logger logger = LoggerFactory.getLogger(ClientStateView.class);

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
        for (ClientNpc npc : clientsStateMachines) {
            try {
                npc.tick();
            } catch (Exception e) {
                logger.error("Erreur inattendue lors du tick du NPC dans le camping {}",
                        campsite.getUniqueID(), e);
            }
        }
    }
}
