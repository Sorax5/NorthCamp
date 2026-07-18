package fr.phylisiumstudio.logic.activity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonIgnore;

import fr.phylisiumstudio.logic.client.Client;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "uniqueID",
        scope = Activity.class
)
public class Activity {
    private UUID uniqueID = UUID.randomUUID();

    private Vector3d position;
    private long duration;
    private double price;
    private int maxClients;
    private ActivityType type;
    private int currentLevel = 0;

    @JsonIgnore
    private List<Client> currentClients = new ArrayList<>();

    public Activity(Vector3d position, long duration, double price, int maxClients, ActivityType type) {
        this.uniqueID = UUID.randomUUID();
        this.position = position;
        this.duration = duration;
        this.price = price;
        this.maxClients = maxClients;
        this.type = type;
        this.currentClients = new ArrayList<>();
    }

    public Activity(UUID uniqueID, Vector3d position, long duration, double price, int maxClients, ActivityType type) {
        this.uniqueID = uniqueID != null ? uniqueID : UUID.randomUUID();
        this.position = position;
        this.duration = duration;
        this.price = price;
        this.maxClients = maxClients;
        this.type = type;
        this.currentClients = new ArrayList<>();
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
