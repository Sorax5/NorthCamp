package fr.phylisiumstudio.logic.client.state;

import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientStateMachine;
import fr.phylisiumstudio.logic.state.Timer;

import java.time.Duration;

public class SleepingState extends ClientState {
    private Timer timer;

    public SleepingState(ClientStateMachine stateMachine) {
        super(stateMachine);
    }

    @Override
    public Client.ClientAction getAction() {
        return Client.ClientAction.SLEEPING;
    }

    @Override
    public void onEnter(Client owner) {
        System.out.println("Entering Sleeping State");
        timer = Timer.builder()
                .duration(Duration.ofSeconds(2))
                .callback(() -> { getStateMachine().HandleTransition(ClientStateMachine.ClientTransition.TO_PLAYING.name()); })
                .build();

        owner.setAction(getAction());
    }

    @Override
    public void onExit(Client owner) {
    }

    @Override
    public void update(Client owner, float deltaTime) {
        timer.Update();
    }
}
