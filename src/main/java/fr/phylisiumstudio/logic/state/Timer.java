package fr.phylisiumstudio.logic.state;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

@Builder
@Getter
public class Timer {
    private final Duration duration;
    private final Instant startTime;

    private final Runnable callback;

    public Timer(Duration duration, Runnable callback) {
        this.duration = duration;
        this.startTime = Instant.now();
        this.callback = callback;
    }

    public boolean IsFinished() {
        return Instant.now().isAfter(startTime.plus(duration));
    }

    public void Update() {
        if (IsFinished()) {
            callback.run();
        }
    }
}
