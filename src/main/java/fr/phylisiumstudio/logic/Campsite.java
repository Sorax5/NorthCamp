package fr.phylisiumstudio.logic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.plot.Plot;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Campsite {
    private final UUID uniqueID;
    private final UUID ownerID;

    private final List<Activity> activities;
    private final List<Plot> plots;
    private final List<Client> clients;

    private double money = 0;

    public Campsite(UUID ownerID) {
        this.uniqueID = UUID.randomUUID();
        this.ownerID = ownerID;
        this.activities = new ArrayList<>();
        this.plots = new ArrayList<>();
        this.clients = new ArrayList<>();
    }

    @JsonCreator
    public Campsite(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("ownerID") UUID ownerID,
            @JsonProperty("activities") List<Activity> activities,
            @JsonProperty("plots") List<Plot> plots,
            @JsonProperty("clients") List<Client> clients,
            @JsonProperty("money") double money
    ) {
        this.uniqueID = uniqueID;
        this.ownerID = ownerID;
        this.activities = activities != null ? new ArrayList<>(activities) : new ArrayList<>();
        this.plots = plots != null ? new ArrayList<>(plots) : new ArrayList<>();
        this.clients = clients != null ? new ArrayList<>(clients) : new ArrayList<>();
        this.money = money;
    }

    public void addActivity(Activity activity) {
        this.activities.add(activity);
    }

    public void addPlot(Plot plot) {
        this.plots.add(plot);
    }

    public void addClient(Client client) {
        this.clients.add(client);
    }

    public void addMoney(double amount) {
        this.money += amount;
    }
}
