package fr.phylisiumstudio.logic.clock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère une {@link GameClock} par camping et pilote son cycle jour/nuit.
 *
 * <p>Chaque horloge est avancée par une tâche répétée (1 tick de jeu = 1 seconde
 * réelle). À chaque transition, l'instance est mise à l'heure Minecraft
 * correspondante et un {@link PhaseChangeEvent} est diffusé sur l'EventHandler
 * global : les autres systèmes réagissent sans coupler l'horloge à leur logique.
 */
@Singleton
public class GameClockService {
    private static final Logger logger = LoggerFactory.getLogger(GameClockService.class);

    private final int dayDurationSeconds;
    private final int nightDurationSeconds;

    private final ConcurrentHashMap<UUID, Handle> handles = new ConcurrentHashMap<>();

    private record Handle(GameClock clock, Task task) {}

    @Inject
    public GameClockService(App app) {
        var config = app.getMainConfig();
        this.dayDurationSeconds = config != null ? config.DayDurationSeconds : 600;
        this.nightDurationSeconds = config != null ? config.NightDurationSeconds : 300;
    }

    /**
     * Démarre l'horloge du camping sur son instance. Idempotent : un appel répété
     * pour le même camping est ignoré.
     */
    public void start(Campsite campsite, Instance instance) {
        handles.computeIfAbsent(campsite.getUniqueID(), _ -> {
            var clock = new GameClock(dayDurationSeconds, nightDurationSeconds);

            // L'horloge de jeu contrôle seule le temps visuel de l'instance.
            instance.setTimeRate(0);
            instance.setTime(clock.getPhase().minecraftTime());

            var task = MinecraftServer.getSchedulerManager()
                    .buildTask(() -> tick(campsite, instance))
                    .repeat(TaskSchedule.seconds(1))
                    .schedule();

            logger.info("Game clock started for campsite {}", campsite.getUniqueID());
            return new Handle(clock, task);
        });
    }

    public void stop(UUID campsiteId) {
        var handle = handles.remove(campsiteId);
        if (handle != null) {
            handle.task().cancel();
            logger.info("Game clock stopped for campsite {}", campsiteId);
        }
    }

    public Optional<GamePhase> getPhase(UUID campsiteId) {
        var handle = handles.get(campsiteId);
        return handle == null ? Optional.empty() : Optional.of(handle.clock().getPhase());
    }

    private void tick(Campsite campsite, Instance instance) {
        var handle = handles.get(campsite.getUniqueID());
        if (handle == null) {
            return;
        }
        var clock = handle.clock();
        if (clock.tick()) {
            instance.setTime(clock.getPhase().minecraftTime());
            EventDispatcher.call(new PhaseChangeEvent(campsite, clock.getPhase(), clock.getDayNumber()));
        }
    }
}
