package fr.phylisiumstudio.logic.clock.event;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.GamePhase;
import net.minestom.server.event.Event;

/**
 * Émis à chaque transition de phase du cycle jour/nuit d'un camping.
 *
 * <p>Point d'extension central (pattern Observer) : la boucle de gameplay,
 * les employés ou les événements saisonniers s'abonnent via l'EventHandler
 * global de Minestom plutôt que de dépendre directement de l'horloge.
 */
public record PhaseChangeEvent(Campsite campsite, GamePhase phase, long dayNumber) implements Event {

    public boolean isDay() {
        return phase == GamePhase.DAY;
    }

    public boolean isNight() {
        return phase == GamePhase.NIGHT;
    }
}
