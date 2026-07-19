package fr.phylisiumstudio.logic.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.phylisiumstudio.logic.plot.Plot;
import lombok.Data;

import java.util.UUID;

@Data
public class Client {
    /** Satisfaction de départ, sur une échelle 0–100. */
    public static final double DEFAULT_SATISFACTION = 70.0;

    private final UUID uniqueID;

    /** Routine micro pilotée par l'arbre de comportement pendant le séjour. */
    private ClientState action;

    /** Étape macro du séjour, pilotée par la boucle quotidienne. */
    private ClientLifecycle lifecycle;

    /** Emplacement affecté ; {@code null} tant que le client patiente à l'accueil. */
    private Plot plot;

    /** Nombre de personnes (1 = client seul, >1 = famille/groupe). */
    private final int groupSize;

    /** Durée de séjour prévue, en jours de jeu. */
    private final int totalStayDays;

    /** Jours de séjour restants avant le départ. */
    private int remainingDays;

    /** Budget dépensable par le client durant tout son séjour. */
    private double budget;

    /** Jauge de satisfaction 0–100 ; influence la fidélité et la réputation. */
    private double satisfaction;

    /**
     * Profil du client (attentes d'activité et sensibilité au prix). Sérialisé via
     * setter ; les anciennes sauvegardes sans ce champ retombent sur {@link ClientArchetype#TOURIST}.
     */
    private ClientArchetype archetype = ClientArchetype.TOURIST;

    /** Client seul, sans séjour planifié — utilisé par les tests et l'ancien flux. */
    public Client(Plot plot) {
        this(UUID.randomUUID(), ClientState.SLEEPY, ClientLifecycle.WAITING, plot, 1, 1, 0.0);
    }

    public Client(int groupSize, int totalStayDays, double budget) {
        this(UUID.randomUUID(), ClientState.SLEEPY, ClientLifecycle.WAITING, null, groupSize, totalStayDays, budget);
    }

    @JsonCreator
    public Client(
            @JsonProperty("uniqueID") UUID uniqueID,
            @JsonProperty("action") ClientState action,
            @JsonProperty("lifecycle") ClientLifecycle lifecycle,
            @JsonProperty("plot") Plot plot,
            @JsonProperty("groupSize") int groupSize,
            @JsonProperty("totalStayDays") int totalStayDays,
            @JsonProperty("budget") double budget
    ) {
        this.uniqueID = uniqueID;
        this.action = action;
        this.lifecycle = lifecycle != null ? lifecycle : ClientLifecycle.WAITING;
        this.plot = plot;
        this.groupSize = Math.max(1, groupSize);
        this.totalStayDays = Math.max(1, totalStayDays);
        this.remainingDays = this.totalStayDays;
        this.budget = budget;
        this.satisfaction = DEFAULT_SATISFACTION;
    }

    /** Un groupe de plus d'une personne nécessite un emplacement de niveau supérieur. */
    public boolean isFamily() {
        return groupSize > 1;
    }

    public enum ClientState {
        SLEEPY,
        BORED,
        CHILL
    }
}
