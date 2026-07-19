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

    /** Fournitures en stock (appât, charbon…), consommées à chaque passage si le type l'exige. */
    private int supplies = 0;

    /** Passages accumulés depuis la dernière maintenance ; l'usure finit par mettre en panne. */
    private int usage = 0;

    /** Activité opérationnelle : indisponible tant qu'un employé ne l'a pas entretenue. */
    private boolean operational = true;

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

    /** Nombre de passages avant panne pour usure (depuis la dernière maintenance). */
    public static final int WEAR_THRESHOLD = 15;

    /**
     * Enregistre un passage client : incrémente l'usure et, au seuil, met l'activité
     * en panne (elle devra être entretenue par la maintenance).
     *
     * @return {@code true} si ce passage vient de provoquer la panne.
     */
    public boolean recordUsage() {
        usage++;
        if (operational && usage >= WEAR_THRESHOLD) {
            operational = false;
            return true;
        }
        return false;
    }

    /** Remet l'activité en service et réinitialise l'usure (maintenance). */
    public void repair() {
        this.operational = true;
        this.usage = 0;
    }

    /** L'activité a-t-elle de quoi fonctionner (stock suffisant, ou type sans consommable) ? */
    public boolean hasSupplies() {
        return !type.consumesSupplies() || supplies > 0;
    }

    /**
     * Consomme une fourniture pour un passage. Sans effet si le type ne consomme rien.
     *
     * @return {@code true} si le passage peut avoir lieu (stock disponible ou non requis).
     */
    public boolean consumeSupply() {
        if (!type.consumesSupplies()) {
            return true;
        }
        if (supplies <= 0) {
            return false;
        }
        supplies--;
        return true;
    }
}

