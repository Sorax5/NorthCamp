package fr.phylisiumstudio.logic.client.Action;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.ai.btree.Task;
import fr.phylisiumstudio.logic.client.ClientEntity;
import net.minestom.server.coordinate.Pos;

import java.time.Duration;
import java.time.Instant;

public class GoToTask extends LeafTask<ClientEntity> {
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final Duration STUCK_THRESHOLD = Duration.ofSeconds(5);
    private static final double STUCK_DISTANCE = 0.5;

    private Instant startTime;
    private Instant lastMoveTime;
    private Pos lastPosition;

    @Override
    public Status execute() {
        var memory = getObject().getMemory();
        var entity = memory.getPlayerEntity();
        var target = memory.getTargetPosition();

        if (entity == null || target == null) {
            resetNavigation();
            return Status.FAILED;
        }

        entity.setCurrentAction(memory.getClient().getAction().toString(), "Going to target position");

        var now = Instant.now();
        if (startTime == null) {
            startTime = now;
            lastMoveTime = now;
            lastPosition = entity.getPosition();
        }

        if (Duration.between(startTime, now).compareTo(TIMEOUT) >= 0) {
            memory.setTargetPosition(null);
            resetNavigation();
            return Status.SUCCEEDED;
        }

        var currentPos = entity.getPosition();
        if (lastPosition != null && currentPos.distance(lastPosition) > STUCK_DISTANCE) {
            lastMoveTime = now;
            lastPosition = currentPos;
        }

        if (Duration.between(lastMoveTime, now).compareTo(STUCK_THRESHOLD) >= 0) {
            memory.setTargetPosition(null);
            resetNavigation();
            return Status.SUCCEEDED;
        }

        var navigator = entity.getNavigator();
        var goal = navigator.getGoalPosition();

        if (goal == null || goal.distance(target) > 0.25) {
            navigator.setPathTo(target);
        }

        if (currentPos.distance(target) < 1.5) {
            memory.setTargetPosition(null);
            resetNavigation();
            return Status.SUCCEEDED;
        }

        return Status.RUNNING;
    }

    public void resetNavigation() {
        startTime = null;
        lastMoveTime = null;
        lastPosition = null;
    }

    @Override
    protected Task<ClientEntity> copyTo(Task<ClientEntity> task) {
        return task;
    }
}
