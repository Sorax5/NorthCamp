package fr.phylisiumstudio.logic.client.state;

import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientStateMachine;
import fr.phylisiumstudio.logic.state.Timer;
import lombok.Getter;

import java.time.Duration;

public class PlayingState extends ClientState {

    @Getter
    private Timer timer;

    public PlayingState(ClientStateMachine stateMachine) {
        super(stateMachine);
    }

    @Override
    public Client.ClientAction getAction() {
        return Client.ClientAction.PLAYING;
    }

    @Override
    public void onEnter(Client owner) {
        this.timer = Timer.builder()
                .duration(Duration.ofSeconds(20))
                .callback(() -> { getStateMachine().HandleTransition(ClientStateMachine.ClientTransition.TO_SLEEPING.name()); })
                .build();
    }

    @Override
    public void onExit(Client owner) {

    }

    @Override
    public void update(Client owner, float deltaTime) {

    }
}
