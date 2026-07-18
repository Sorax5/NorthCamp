package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientMemory;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;

import java.time.Duration;
import java.time.Instant;

/**
 * Déplace le NPC vers {@code memory.targetPosition} via le pathfinder Minestom.
 *
 * <p>Le blocage est jugé sur la <b>progression vers la cible</b> : tant que le
 * NPC se rapproche, il n'est jamais interrompu (aucune téléportation en pleine
 * course). S'il cesse d'avancer, la tâche tente plusieurs déblocages successifs
 * (recalcul de chemin + petit coup de pouce). La téléportation à destination
 * n'intervient qu'en tout dernier recours, une fois ces tentatives épuisées, de
 * sorte que la machine à états ne gèle jamais.
 */
public class GoToTask extends LeafTask<ClientEntity> {

    private static final double ARRIVAL_DISTANCE = 1.5;
    /** Tolérance pour savoir si le navigator vise déjà la bonne cible. */
    private static final double GOAL_TOLERANCE = 0.25;

    /** Fenêtre d'évaluation de la progression. */
    private static final Duration PROGRESS_WINDOW = Duration.ofMillis(2500);
    /** Distance minimale de rapprochement sur la fenêtre pour être « en progression ». */
    private static final double MIN_PROGRESS = 0.6;
    /** Nombre de déblocages tentés avant de forcer l'arrivée par téléportation. */
    private static final int MAX_UNBLOCK_ATTEMPTS = 4;
    /** Délai minimal entre deux recalculs de chemin. */
    private static final Duration REPATH_COOLDOWN = Duration.ofMillis(800);

    private Instant windowStart;
    private double windowStartDistance;
    private int unblockAttempts;
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

        var now = Instant.now();
        var currentPos = entity.getPosition();
        double distance = currentPos.distance(target);

        if (windowStart == null) {
            windowStart = now;
            windowStartDistance = distance;
            unblockAttempts = 0;
            lastRepathTime = Instant.EPOCH; // premier calcul immédiat
        }

        if (distance < ARRIVAL_DISTANCE) {
            return arrive(memory);
        }

        entity.setCurrentAction("Going to " + formatPos(target));
        ensurePath(entity, target, now);

        // Évaluation périodique de la progression vers la cible.
        if (elapsed(windowStart, now).compareTo(PROGRESS_WINDOW) >= 0) {
            double progress = windowStartDistance - distance;
            if (progress >= MIN_PROGRESS) {
                // Le NPC se rapproche : tout va bien, on oublie les tentatives.
                unblockAttempts = 0;
            } else {
                // Il n'avance plus : on tente de le débloquer, téléport en dernier recours.
                unblockAttempts++;
                if (unblockAttempts >= MAX_UNBLOCK_ATTEMPTS) {
                    return forceArrival(memory, entity, target);
                }
                unblock(entity, target, now);
            }
            windowStart = now;
            windowStartDistance = distance;
        }

        return Status.RUNNING;
    }

    /** (Re)calcule le chemin si le navigator ne vise pas déjà la cible. */
    private void ensurePath(ClientEntity entity, Pos target, Instant now) {
        var navigator = entity.getNavigator();
        var goal = navigator.getGoalPosition();
        boolean needsPath = goal == null || goal.distance(target) > GOAL_TOLERANCE;
        if (needsPath && elapsed(lastRepathTime, now).compareTo(REPATH_COOLDOWN) >= 0) {
            lastRepathTime = now;
            navigator.setPathTo(target);
        }
    }

    /** Tentative de déblocage : nouveau chemin et léger coup de pouce pour se dégager. */
    private void unblock(ClientEntity entity, Pos target, Instant now) {
        entity.setCurrentAction("Unblocking (" + unblockAttempts + "/" + MAX_UNBLOCK_ATTEMPTS + ")");
        var navigator = entity.getNavigator();
        navigator.reset();
        lastRepathTime = now;
        navigator.setPathTo(target);

        // Petite impulsion vers la cible (et vers le haut) pour franchir un rebord.
        var direction = target.sub(entity.getPosition()).asVec().normalize();
        entity.setVelocity(new Vec(direction.x() * 3.0, 3.0, direction.z() * 3.0));
    }

    private Status arrive(ClientMemory memory) {
        memory.setTargetPosition(null);
        reset();
        return Status.SUCCEEDED;
    }

    /** Dernier recours : téléporte à destination pour ne jamais figer la machine à états. */
    private Status forceArrival(ClientMemory memory, ClientEntity entity, Pos target) {
        entity.getNavigator().reset();
        entity.teleport(target);
        entity.setCurrentAction("Arrived (unblock failed)");
        memory.setTargetPosition(null);
        reset();
        return Status.SUCCEEDED;
    }

    public void reset() {
        windowStart = null;
        windowStartDistance = 0;
        unblockAttempts = 0;
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
