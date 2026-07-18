package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientMemory;
import net.minestom.server.coordinate.Pos;

import java.time.Duration;
import java.time.Instant;

/**
 * Déplace le NPC vers {@code memory.targetPosition} via le pathfinder Minestom.
 *
 * <p>Robuste par conception : si le chemin est introuvable ou si le NPC reste
 * bloqué, la tâche ne gèle jamais la boucle de comportement — après un délai de
 * grâce elle téléporte le NPC à destination et réussit. Cela garantit que la
 * machine à états avance toujours (sommeil → activité → chill → …) même quand le
 * terrain met le pathfinder en défaut.
 */
public class GoToTask extends LeafTask<ClientEntity> {

    /** Durée max avant de forcer l'arrivée par téléportation. */
    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    /** Fenêtre d'observation pour détecter un blocage. */
    private static final Duration STUCK_CHECK_WINDOW = Duration.ofSeconds(2);
    /** Distance minimale attendue sur la fenêtre pour ne pas être considéré bloqué. */
    private static final double STUCK_MIN_DISTANCE = 0.3;
    /** Tentatives de recalcul de chemin avant de forcer l'arrivée. */
    private static final int MAX_REPATH_ATTEMPTS = 3;
    /** Délai entre deux recalculs de chemin. */
    private static final Duration REPATH_COOLDOWN = Duration.ofMillis(800);
    /** Échecs consécutifs de recherche de chemin avant de forcer l'arrivée. */
    private static final int MAX_PATH_FAILURES = 3;

    private static final double ARRIVAL_DISTANCE = 1.5;
    /** Tolérance pour savoir si le navigator vise déjà la bonne cible. */
    private static final double GOAL_TOLERANCE = 0.25;

    private Instant startTime;
    private Instant stuckWindowStart;
    private Pos stuckWindowPos;
    private int repathAttempts;
    private int pathFailures;
    private Instant lastRepathTime;

    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        var target = memory.getTargetPosition();

        if (entity == null || target == null) {
            reset();
            return Status.FAILED;
        }

        entity.setCurrentAction("Going to " + formatPos(target));

        var now = Instant.now();
        var currentPos = entity.getPosition();

        if (startTime == null) {
            startTime = now;
            stuckWindowStart = now;
            stuckWindowPos = currentPos;
            repathAttempts = 0;
            pathFailures = 0;
            lastRepathTime = Instant.EPOCH; // force un premier calcul immédiat
        }

        // Arrivée.
        if (currentPos.distance(target) < ARRIVAL_DISTANCE) {
            return arrive(memory);
        }

        // Délai de grâce dépassé : on force l'arrivée plutôt que de geler la boucle.
        if (elapsed(startTime, now).compareTo(TIMEOUT) >= 0) {
            return forceArrival(memory, entity, target, "timeout");
        }

        // Détection de blocage sur fenêtre glissante.
        if (elapsed(stuckWindowStart, now).compareTo(STUCK_CHECK_WINDOW) >= 0) {
            double moved = currentPos.distance(stuckWindowPos);
            if (moved < STUCK_MIN_DISTANCE && !tryRepath(entity, target, now)) {
                return forceArrival(memory, entity, target, "stuck");
            }
            stuckWindowStart = now;
            stuckWindowPos = currentPos;
        }

        // (Re)calcul du chemin si le navigator ne vise pas déjà la bonne cible.
        var navigator = entity.getNavigator();
        var goal = navigator.getGoalPosition();
        if ((goal == null || goal.distance(target) > GOAL_TOLERANCE)
                && elapsed(lastRepathTime, now).compareTo(REPATH_COOLDOWN) >= 0) {
            lastRepathTime = now;
            boolean found = navigator.setPathTo(target);
            if (!found && ++pathFailures >= MAX_PATH_FAILURES) {
                return forceArrival(memory, entity, target, "no path");
            }
        }

        return Status.RUNNING;
    }

    private Status arrive(ClientMemory memory) {
        memory.setTargetPosition(null);
        reset();
        return Status.SUCCEEDED;
    }

    /** Force l'arrivée par téléportation pour ne jamais bloquer la machine à états. */
    private Status forceArrival(ClientMemory memory, ClientEntity entity, Pos target, String reason) {
        entity.getNavigator().reset();
        entity.teleport(target);
        entity.setCurrentAction("Arrived (" + reason + ")");
        memory.setTargetPosition(null);
        reset();
        return Status.SUCCEEDED;
    }

    private boolean tryRepath(ClientEntity entity, Pos target, Instant now) {
        if (repathAttempts >= MAX_REPATH_ATTEMPTS) {
            return false;
        }
        if (elapsed(lastRepathTime, now).compareTo(REPATH_COOLDOWN) < 0) {
            return true; // on attend le cooldown, pas encore épuisé
        }
        repathAttempts++;
        lastRepathTime = now;
        entity.getNavigator().setPathTo(target);
        return true;
    }

    public void reset() {
        startTime = null;
        stuckWindowStart = null;
        stuckWindowPos = null;
        repathAttempts = 0;
        pathFailures = 0;
        lastRepathTime = null;
    }

    private static Duration elapsed(Instant from, Instant to) {
        return Duration.between(from, to);
    }

    private static String formatPos(Pos p) {
        return String.format("(%.1f, %.1f, %.1f)", p.x(), p.y(), p.z());
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
