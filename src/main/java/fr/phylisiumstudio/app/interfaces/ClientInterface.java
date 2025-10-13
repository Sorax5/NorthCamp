package fr.phylisiumstudio.app.interfaces;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientStateMachine;
import lombok.Getter;

import java.util.List;

public class ClientInterface {
    @Getter
    private final Campsite campsite;

    private final List<ClientStateMachine> clientsStateMachines;

    public ClientInterface(Campsite campsite) {
        this.campsite = campsite;
        this.clientsStateMachines = campsite.getClients().stream().map(ClientStateMachine::new).toList();
    }

    public void Update(float deltaTime) {
        clientsStateMachines.forEach(stateMachine -> stateMachine.Update(deltaTime));
    }
}
