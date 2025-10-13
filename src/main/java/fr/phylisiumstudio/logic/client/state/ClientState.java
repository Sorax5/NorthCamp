package fr.phylisiumstudio.logic.client.state;

import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientStateMachine;
import fr.phylisiumstudio.state.State;
import lombok.Getter;

public abstract class ClientState implements State<Client> {
    @Getter
    private final ClientStateMachine stateMachine;

    public ClientState(ClientStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    @Override
    public String getId() {
        return getAction().name();
    }

    public abstract Client.ClientAction getAction();
}
