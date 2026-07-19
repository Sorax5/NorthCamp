package fr.phylisiumstudio.logic.clock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.App;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    /** Ticks de jeu par seconde réelle (cadence Minestom). */
    private static final int TICKS_PER_SECOND = 20;

    private final ConcurrentHashMap<UUID, Handle> handles = new ConcurrentHashMap<>();

    private record Handle(GameClock clock, Task task, AtomicInteger subTick) {}

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

            // L'horloge de jeu contrôle seule le temps visuel : on fige la progression
            // naturelle (rate 0) et on pilote l'heure nous-mêmes à chaque tick.
            instance.setTimeRate(0);
            instance.setTime(clock.minecraftTime());

            // Planifié sur l'ordonnanceur de l'instance : le tick d'horloge s'exécute
            // sur le thread de tick de l'instance, donc l'accès au temps de l'instance
            // et la diffusion de l'événement sont thread-safe par construction.
            // Cadence au tick (et non à la seconde) pour un ciel fluide sans à-coups.
            var task = instance.scheduler().submitTask(() -> {
                tick(campsite, instance);
                return TaskSchedule.tick(1);
            });

            logger.info("Game clock started for campsite {}", campsite.getUniqueID());
            return new Handle(clock, task, new AtomicInteger(0));
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
        int sub = handle.subTick().incrementAndGet();

        if (sub >= TICKS_PER_SECOND) {
            // Une seconde de jeu écoulée : l'horloge avance (phases, numéro de jour).
            handle.subTick().set(0);
            boolean transitioned = clock.tick();
            instance.setTime(clock.minecraftTime());
            if (transitioned) {
                announce(instance, clock);
                EventDispatcher.call(new PhaseChangeEvent(campsite, instance, clock.getPhase(), clock.getDayNumber()));
            }
        } else {
            // Entre deux secondes : ciel interpolé pour un défilement fluide.
            // ponytail: 20 maj/s par instance ; throttler à 1 tick sur 2 si le réseau serre.
            instance.setTime(clock.minecraftTime(sub / (double) TICKS_PER_SECOND));
        }
    }

    /** Informe les joueurs de l'instance de la transition (l'instance est une Audience). */
    private void announce(Instance instance, GameClock clock) {
        var text = clock.getPhase() == GamePhase.DAY
                ? Component.text("☀ Jour " + clock.getDayNumber(), NamedTextColor.GOLD)
                : Component.text("🌙 La nuit tombe", NamedTextColor.BLUE);
        instance.sendMessage(text);
    }
}
