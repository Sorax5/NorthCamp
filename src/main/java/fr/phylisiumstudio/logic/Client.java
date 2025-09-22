package fr.phylisiumstudio.logic;

import lombok.Getter;

import java.util.UUID;

public class Client {
    @Getter
    private final UUID uniqueID;

    public Client() {
        this.uniqueID = UUID.randomUUID();
    }
}
