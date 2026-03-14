package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import fr.phylisiumstudio.logic.client.ClientEntity;

import java.time.Duration;
import java.time.Instant;

/**
 * Classe abstraite pour les LeafTask qui dépendent d'une durée.
 * Gère automatiquement le démarrage du timer, la vérification de l'écoulement
 * du temps, et le reset à la fin.
 */
public abstract class TimedLeafTask extends LeafTask<ClientEntity> {
    private Instant startTime;

    /**
     * Retourne la durée pendant laquelle la tâche doit s'exécuter.
     */
    protected abstract Duration getDuration();

    /**
     * Appelé une seule fois au début de la tâche (quand le timer démarre).
     * Permet d'initialiser l'état de la tâche.
     * @return true si la tâche peut démarrer, false pour échouer (Status.FAILED)
     */
    protected boolean onStart() {
        return true;
    }

    /**
     * Appelé à chaque tick pendant que la tâche est en cours.
     * Permet de mettre à jour l'état visuel ou logique du NPC.
     */
    protected void onRunning() {
    }

    /**
     * Appelé une seule fois quand la durée est écoulée.
     * Permet de nettoyer l'état de la tâche.
     */
    protected void onEnd() {
    }

    protected Duration getTimeLeft() {
        if (startTime == null) {
            return getDuration();
        }
        var elapsed = Duration.between(startTime, Instant.now());
        var timeLeft = getDuration().minus(elapsed);
        return timeLeft.isNegative() ? Duration.ZERO : timeLeft;
    }

    @Override
    public Status execute() {
        if (startTime == null) {
            if (!onStart()) {
                return Status.FAILED;
            }
            startTime = Instant.now();
            return Status.RUNNING;
        }

        onRunning();

        if (Duration.between(startTime, Instant.now()).compareTo(getDuration()) >= 0) {
            startTime = null;
            onEnd();
            return Status.SUCCEEDED;
        }

        return Status.RUNNING;
    }

    /**
     * Réinitialise le timer manuellement si nécessaire.
     */
    protected void resetTimer() {
        startTime = null;
    }
}

