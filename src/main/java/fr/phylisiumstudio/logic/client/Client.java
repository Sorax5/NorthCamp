package fr.phylisiumstudio.logic.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.phylisiumstudio.logic.plot.Plot;
import lombok.Data;

import java.util.UUID;

@Data
public class Client {
    private final UUID uniqueID;
    private ClientState action;
    private final Plot plot;

    public Client(Plot plot) {
        this.uniqueID = UUID.randomUUID();
        this.action = ClientState.SLEEPY;
        this.plot = plot;
    }

    @JsonCreator
    public Client(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("action") ClientState action,
            @JsonProperty("plot") Plot plot
    ) {
        this.uniqueID = uniqueID;
        this.action = action;
        this.plot = plot;
    }

    public enum ClientState {
        SLEEPY,
        BORED,
        CHILL
    }
}
