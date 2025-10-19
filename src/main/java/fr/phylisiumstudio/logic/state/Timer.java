package fr.phylisiumstudio.logic.state;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Builder
@Getter
public class Timer {
    private final Duration duration;
    private final Instant startTime = Instant.now();

    private final Runnable callback;

    public boolean IsFinished() {
        return Instant.now().isAfter(startTime.plus(duration));
    }

    public void Update() {
        if (IsFinished()) {
            callback.run();
        }
    }
}
