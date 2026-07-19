package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.SatisfactionService;

import java.util.Random;

/**
 * Déclenche aléatoirement un {@link CampEvent} au lever du jour et applique son
 * effet sur le camping. Introduit de la variété et de la tension : l'orage force
 * la maintenance, l'ours punit un camping mal tenu, le festival récompense la
 * réputation.
 */
@Singleton
public class EventService {

    /** Probabilité qu'un événement survienne un jour donné. */
    private static final double EVENT_CHANCE = 0.20;
    private static final double BEAR_REPUTATION_HIT = 3.0;
    private static final double FESTIVAL_REPUTATION_BONUS = 5.0;

    private final Random random;

    @Inject
    public EventService(Random random) {
        this.random = random;
    }

    /**
     * Tente de déclencher un événement pour ce camping.
     *
     * @return l'événement survenu, ou {@code null} si la journée est calme.
     */
    public CampEvent maybeTrigger(Campsite campsite) {
        if (random.nextDouble() >= EVENT_CHANCE) {
            return null;
        }
        var event = CampEvent.values()[random.nextInt(CampEvent.values().length)];
        apply(campsite, event);
        return event;
    }

    private void apply(Campsite campsite, CampEvent event) {
        switch (event) {
            case STORM -> campsite.getActivities().forEach(a -> a.setOperational(false));
            case BEAR -> {
                campsite.adjustReputation(-BEAR_REPUTATION_HIT);
                campsite.getClients().stream()
                        .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING)
                        .forEach(SatisfactionService::applyScare);
            }
            case FESTIVAL -> campsite.adjustReputation(FESTIVAL_REPUTATION_BONUS);
        }
    }
}
