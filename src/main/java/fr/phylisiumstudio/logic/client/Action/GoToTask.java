package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientMemory;
import net.minestom.server.coordinate.Pos;

import java.time.Duration;
import java.time.Instant;

public class GoToTask extends LeafTask<ClientEntity> {

    // ── Timeouts ──────────────────────────────────────────────────────────────
    /** Durée max totale avant d'abandonner la navigation */
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    // ── Détection de blocage ──────────────────────────────────────────────────
    /** Fenêtre d'observation pour détecter un blocage */
    private static final Duration STUCK_CHECK_WINDOW = Duration.ofSeconds(2);
    /** Distance minimale attendue sur la fenêtre pour ne pas être considéré bloqué */
    private static final double STUCK_MIN_DISTANCE = 0.3;
    /** Nombre de tentatives de repath avant d'abandonner */
    private static final int MAX_REPATH_ATTEMPTS = 3;
    /** Délai entre deux tentatives de repath */
    private static final Duration REPATH_COOLDOWN = Duration.ofMillis(800);

    // ── Critère d'arrivée ─────────────────────────────────────────────────────
    private static final double ARRIVAL_DISTANCE = 1.5;
    /** Tolérance pour savoir si le navigator vise déjà la bonne cible */
    private static final double GOAL_TOLERANCE = 0.25;

    // ── État interne ──────────────────────────────────────────────────────────
    private Instant startTime;
    private Instant stuckWindowStart;
    private Pos stuckWindowPos;
    private int repathAttempts;
    private Instant lastRepathTime;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Status execute() {
        var memory  = getObject().getMemory();
        var entity  = memory.getPlayerEntity();
        var target  = memory.getTargetPosition();

        if (entity == null || target == null) {
            reset();
            return Status.FAILED;
        }

        entity.setCurrentAction("Going to " + formatPos(target));

        var now        = Instant.now();
        var currentPos = entity.getPosition();

        // ── Initialisation au premier tick ───────────────────────────────────
        if (startTime == null) {
            startTime        = now;
            stuckWindowStart = now;
            stuckWindowPos   = currentPos;
            repathAttempts   = 0;
            lastRepathTime   = now;
        }

        // ── Timeout global ───────────────────────────────────────────────────
        if (elapsed(startTime, now).compareTo(TIMEOUT) >= 0) {
            return failWith(memory, "timeout");
        }

        // ── Critère d'arrivée ────────────────────────────────────────────────
        if (currentPos.distance(target) < ARRIVAL_DISTANCE) {
            memory.setTargetPosition(null);
            reset();
            return Status.SUCCEEDED;
        }

        // ── Détection de blocage (fenêtre glissante) ─────────────────────────
        boolean windowExpired = elapsed(stuckWindowStart, now).compareTo(STUCK_CHECK_WINDOW) >= 0;
        if (windowExpired) {
            double moved = currentPos.distance(stuckWindowPos);
            if (moved < STUCK_MIN_DISTANCE) {
                // Le NPC n'a pas assez bougé sur la fenêtre → bloqué
                if (!tryRepath(entity, target, now)) {
                    // Plus de tentatives disponibles → on abandonne
                    return failWith(memory, "stuck - repath exhausted");
                }
            }
            // Renouvelle la fenêtre dans tous les cas
            stuckWindowStart = now;
            stuckWindowPos   = currentPos;
        }

        // ── Mise à jour de la cible de navigation ────────────────────────────
        var navigator = entity.getNavigator();
        var goal      = navigator.getGoalPosition();
        if (goal == null || goal.distance(target) > GOAL_TOLERANCE) {
            navigator.setPathTo(target);
        }

        return Status.RUNNING;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tente un repath si le cooldown le permet et si des tentatives restent.
     * @return {@code true} si un repath a été lancé, {@code false} si épuisé.
     */
    private boolean tryRepath(ClientEntity entity, Pos target, Instant now) {
        if (repathAttempts >= MAX_REPATH_ATTEMPTS) {
            return false;
        }
        // Respecte un cooldown pour ne pas spammer le pathfinder
        if (elapsed(lastRepathTime, now).compareTo(REPATH_COOLDOWN) < 0) {
            return true; // on attend, mais on ne considère pas ça comme épuisé
        }
        repathAttempts++;
        lastRepathTime = now;
        // Force un nouveau calcul de chemin
        entity.getNavigator().setPathTo(target);
        return true;
    }

    /**
     * Abandonne la navigation, nettoie la mémoire et retourne FAILED.
     */
    private Status failWith(ClientMemory memory, String reason) {
        memory.setTargetPosition(null);
        reset();
        return Status.FAILED;
    }

    public void reset() {
        startTime = null;
        stuckWindowStart = null;
        stuckWindowPos = null;
        repathAttempts = 0;
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