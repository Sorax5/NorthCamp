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

    /** Réputation de départ d'un nouveau camping, sur une échelle 0–100. */
    public static final double DEFAULT_REPUTATION = 50.0;

    private final List<Activity> activities;
    private final List<Plot> plots;
    private final List<Client> clients;

    private double money = 0;

    /** Réputation du camping (0–100) ; attire plus ou moins de clients. */
    private double reputation = DEFAULT_REPUTATION;

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
            @JsonProperty("money") double money,
            @JsonProperty("reputation") double reputation
    ) {
        this.uniqueID = uniqueID;
        this.ownerID = ownerID;
        this.activities = activities != null ? new ArrayList<>(activities) : new ArrayList<>();
        this.plots = plots != null ? new ArrayList<>(plots) : new ArrayList<>();
        this.clients = clients != null ? new ArrayList<>(clients) : new ArrayList<>();
        this.money = money;
        this.reputation = reputation;
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

    /** Ajuste la réputation en la maintenant dans l'intervalle [0, 100]. */
    public void adjustReputation(double delta) {
        this.reputation = Math.max(0.0, Math.min(100.0, this.reputation + delta));
    }
}
