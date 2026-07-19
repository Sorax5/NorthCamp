package fr.phylisiumstudio.logic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.amenity.Amenity;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.staff.Staff;
import lombok.Getter;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class Campsite {
    private final UUID uniqueID;
    private final UUID ownerID;

    /** Réputation de départ d'un nouveau camping, sur une échelle 0–100. */
    public static final double DEFAULT_REPUTATION = 50.0;

    private final List<Activity> activities;
    private final List<Plot> plots;
    private final List<Client> clients;
    private final List<Staff> staff;

    /** Aménagements construits (sanitaires, épicerie…), au bénéfice de tout le camping. */
    private final Set<Amenity> amenities;

    private double money = 0;

    /** Réputation du camping (0–100) ; attire plus ou moins de clients. */
    private double reputation = DEFAULT_REPUTATION;

    public Campsite(UUID ownerID) {
        this.uniqueID = UUID.randomUUID();
        this.ownerID = ownerID;
        this.activities = new CopyOnWriteArrayList<>();
        this.plots = new CopyOnWriteArrayList<>();
        this.clients = new CopyOnWriteArrayList<>();
        this.staff = new CopyOnWriteArrayList<>();
        this.amenities = EnumSet.noneOf(Amenity.class);
    }

    @JsonCreator
    public Campsite(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("ownerID") UUID ownerID,
            @JsonProperty("activities") List<Activity> activities,
            @JsonProperty("plots") List<Plot> plots,
            @JsonProperty("clients") List<Client> clients,
            @JsonProperty("staff") List<Staff> staff,
            @JsonProperty("money") double money,
            @JsonProperty("reputation") double reputation,
            @JsonProperty("amenities") Collection<Amenity> amenities
    ) {
        this.uniqueID = uniqueID;
        this.ownerID = ownerID;
        this.activities = new CopyOnWriteArrayList<>(activities != null ? activities : List.of());
        this.plots = new CopyOnWriteArrayList<>(plots != null ? plots : List.of());
        this.clients = new CopyOnWriteArrayList<>(clients != null ? clients : List.of());
        this.staff = new CopyOnWriteArrayList<>(staff != null ? staff : List.of());
        this.money = money;
        this.reputation = reputation;
        // Ancienne sauvegarde sans aménagements : ensemble vide par défaut.
        this.amenities = (amenities == null || amenities.isEmpty())
                ? EnumSet.noneOf(Amenity.class)
                : EnumSet.copyOf(amenities);
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

    public void addStaff(Staff staff) {
        this.staff.add(staff);
    }

    public boolean hasAmenity(Amenity amenity) {
        return this.amenities.contains(amenity);
    }

    public void addAmenity(Amenity amenity) {
        this.amenities.add(amenity);
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    /** Ajuste la réputation en la maintenant dans l'intervalle [0, 100]. */
    public void adjustReputation(double delta) {
        this.reputation = Math.max(0.0, Math.min(100.0, this.reputation + delta));
    }
}
