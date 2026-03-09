package fr.phylisiumstudio.logic.activity;

import com.fasterxml.jackson.annotation.*;

import fr.phylisiumstudio.logic.client.Client;
import lombok.Data;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uniqueID",
        scope = Activity.class
)
public class Activity {
    private final UUID uniqueID;

    private final Vector3d position;
    private final long duration;
    private final double price;
    private final int maxClients;
    private final ActivityType type;

    @JsonIgnore
    private List<Client> currentClients = new ArrayList<>();

    @JsonCreator
    public Activity(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("position") Vector3d position,
            @JsonProperty("duration") long duration,
            @JsonProperty("price") double price,
            @JsonProperty("maxClients") int maxClients,
            @JsonProperty("type") ActivityType type
    ) {
        this.uniqueID = uniqueID;
        this.position = position;
        this.duration = duration;
        this.price = price;
        this.maxClients = maxClients;
        this.type = type;
    }

    public Activity(Vector3d position, long duration, double price, int maxClients, ActivityType type) {
        this.uniqueID = UUID.randomUUID();
        this.position = position;
        this.duration = duration;
        this.price = price;
        this.maxClients = maxClients;
        this.type = type;
    }

    public boolean addClient(Client client) {
        if (currentClients.size() < maxClients) {
            currentClients.add(client);
            return true;
        }
        return false;
    }

    public void removeClient(Client client) {
        currentClients.remove(client);
    }
}
