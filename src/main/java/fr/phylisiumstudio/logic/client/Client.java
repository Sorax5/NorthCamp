package fr.phylisiumstudio.logic.client;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class Client {
    @Getter
    private final UUID uniqueID;

    @Getter
    @Setter
    private ClientAction action;

    public Client() {
        this.uniqueID = UUID.randomUUID();
    }

    public enum ClientAction {
        SLEEPING,
        PLAYING
    }
}
