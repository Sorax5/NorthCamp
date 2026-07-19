package fr.phylisiumstudio.logic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.amenity.Amenity;
import fr.phylisiumstudio.logic.amenity.AmenityInstance;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.staff.Staff;
import lombok.Getter;

import java.util.List;
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
    private final List<AmenityInstance> builtAmenities;

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
        this.builtAmenities = new CopyOnWriteArrayList<>();
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
            @JsonProperty("builtAmenities") List<AmenityInstance> amenities
    ) {
        this.uniqueID = uniqueID;
        this.ownerID = ownerID;
        this.activities = new CopyOnWriteArrayList<>(activities != null ? activities : List.of());
        this.plots = new CopyOnWriteArrayList<>(plots != null ? plots : List.of());
        this.clients = new CopyOnWriteArrayList<>(clients != null ? clients : List.of());
        this.staff = new CopyOnWriteArrayList<>(staff != null ? staff : List.of());
        this.money = money;
        this.reputation = reputation;
        // Champ renommé « builtAmenities » : l'ancien « amenities » (noms d'enum)
        // devient une propriété inconnue ignorée → migration sans perte du camping.
        this.builtAmenities = new CopyOnWriteArrayList<>(amenities != null ? amenities : List.of());
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
        return this.builtAmenities.stream().anyMatch(a -> a.type() == amenity);
    }

    public void addAmenity(AmenityInstance amenity) {
        this.builtAmenities.add(amenity);
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    /** Ajuste la réputation en la maintenant dans l'intervalle [0, 100]. */
    public void adjustReputation(double delta) {
        this.reputation = Math.max(0.0, Math.min(100.0, this.reputation + delta));
    }
}
