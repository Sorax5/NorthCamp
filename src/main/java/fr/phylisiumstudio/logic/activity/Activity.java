package fr.phylisiumstudio.logic.activity;

import com.fasterxml.jackson.annotation.*;
import fr.phylisiumstudio.logic.activity.fabric.ActivityDataFabric;
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

    @JsonIgnore
    private final ActivityData activityData;

    private final Vector3d position;
    private final long duration;
    private final double price;
    private final int maxClients;

    @JsonIgnore
    private List<Client> currentClients = new ArrayList<>();

    @JsonProperty("type")
    public ActivityType getType() {
        return activityData != null ? activityData.type() : null;
    }

    @JsonCreator
    public Activity(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("position") Vector3d position,
            @JsonProperty("duration") long duration,
            @JsonProperty("price") double price,
            @JsonProperty("maxClients") int maxClients,
            @JsonProperty("type") ActivityType type,
            @JacksonInject ActivityDataFabric activityDataFabric
    ) {
        this.uniqueID = uniqueID;
        this.position = position;
        this.duration = duration;
        this.price = price;
        this.maxClients = maxClients;
        this.activityData = activityDataFabric.getActivityData(type);
    }

    public Activity(ActivityData activityData, Vector3d position, long duration, double price, int maxClients) {
        this.uniqueID = UUID.randomUUID();
        this.activityData = activityData;
        this.position = position;
        this.duration = duration;
        this.price = price;
        this.maxClients = maxClients;
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
