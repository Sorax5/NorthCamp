package fr.phylisiumstudio.logic.client;

import fr.phylisiumstudio.logic.client.state.SleepingState;
import fr.phylisiumstudio.state.StateMachine;

public class ClientStateMachine extends StateMachine<Client> {

    /**
     * Constructor for the StateMachine.
     *
     * @param owner The owner of the state machine.
     */
    public ClientStateMachine(Client owner) {
        super(owner);

        this.AddState(new SleepingState(this));

        this.AddTransition(Client.ClientAction.SLEEPING.toString(), ClientTransition.TO_PLAYING.name(), Client.ClientAction.PLAYING.toString());
    }

    public enum ClientTransition {
        TO_SLEEPING,
        TO_PLAYING
    }
}
