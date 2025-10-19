package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientStateMachine;
import lombok.Getter;

import java.util.List;

public class ClientStateView {
    @Getter
    private final Campsite campsite;
    private final List<ClientStateMachine> clientsStateMachines;

    public ClientStateView(Campsite campsite) {
        this.campsite = campsite;
        this.clientsStateMachines = campsite.getClients().stream().map(ClientStateMachine::new).toList();
    }

    public void Update(float deltaTime) {
        clientsStateMachines.forEach(stateMachine -> stateMachine.Update(deltaTime));
    }
}
