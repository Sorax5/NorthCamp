package fr.phylisiumstudio.logic.client;

import fr.phylisiumstudio.logic.client.state.PlayingState;
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
        this.AddState(new PlayingState(this));

        this.AddTransition(Client.ClientAction.SLEEPING.toString(), ClientTransition.TO_PLAYING.name(), Client.ClientAction.PLAYING.toString());
        this.AddTransition(Client.ClientAction.PLAYING.toString(), ClientTransition.TO_SLEEPING.name(), Client.ClientAction.SLEEPING.toString());

        this.SetInitialState(Client.ClientAction.SLEEPING.toString());
    }

    public enum ClientTransition {
        TO_SLEEPING,
        TO_PLAYING
    }
}
