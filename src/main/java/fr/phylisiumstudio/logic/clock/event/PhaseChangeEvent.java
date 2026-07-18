package fr.phylisiumstudio.logic.clock.event;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.GamePhase;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;

/**
 * Émis à chaque transition de phase du cycle jour/nuit d'un camping.
 *
 * <p>C'est un {@link InstanceEvent} : il est routé vers le nœud d'événements de
 * l'instance concernée ({@code instance.eventNode()}), ce qui permet aux vues
 * d'un camping de s'abonner sans recevoir les événements des autres instances,
 * avec un nettoyage automatique quand l'instance est désenregistrée.
 */
public record PhaseChangeEvent(Campsite campsite, Instance instance, GamePhase phase, long dayNumber)
        implements InstanceEvent {

    @Override
    public Instance getInstance() {
        return instance;
    }

    public boolean isDay() {
        return phase == GamePhase.DAY;
    }

    public boolean isNight() {
        return phase == GamePhase.NIGHT;
    }
}
