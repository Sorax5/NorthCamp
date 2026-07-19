package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.App;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.timer.TaskSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code /shutdown} : arrêt propre et garanti du serveur.
 *
 * <p>Diffuse un avertissement puis, après un court délai <b>non bloquant</b>
 * (planifié via le scheduler, jamais {@code Thread.sleep} sur le thread de tick),
 * exécute le teardown sur un thread dédié :
 * <ol>
 *   <li>{@link MinecraftServer#stopCleanly()} — fin du tick, déconnexion des
 *       joueurs, arrêt du réseau ;</li>
 *   <li>{@link App#OnDisable()} — sauvegarde des données et arrêt des sous-systèmes
 *       à threads non-daemon (profiler Spark) qui, sinon, maintiennent la JVM en vie ;</li>
 *   <li>{@link System#exit(int)} — terminaison garantie.</li>
 * </ol>
 *
 * <p>Le teardown tourne hors du thread de tick (déjà stoppé) pour éviter le
 * blocage circulaire de l'ancien {@code System.exit} appelé depuis ce thread.
 */
public class ShutdownCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownCommand.class);
    private static final int COUNTDOWN_SECONDS = 5;

    private final App app;

    @Inject
    public ShutdownCommand(App app) {
        super("shutdown");
        this.app = app;
        setDefaultExecutor((sender, ctx) -> start());
    }

    private void start() {
        broadcast(Component.text("Le serveur s'arrête dans " + COUNTDOWN_SECONDS
                + " secondes. Merci de vous déconnecter.", NamedTextColor.RED, TextDecoration.BOLD));
        logger.info("Shutdown requested — stopping in {} seconds.", COUNTDOWN_SECONDS);

        MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    broadcast(Component.text("Arrêt du serveur.", NamedTextColor.RED, TextDecoration.BOLD));
                    new Thread(this::teardown, "shutdown").start();
                })
                .delay(TaskSchedule.seconds(COUNTDOWN_SECONDS))
                .schedule();
    }

    private void teardown() {
        logger.info("Stopping server cleanly...");
        MinecraftServer.stopCleanly();
        app.OnDisable();
        System.exit(0);
    }

    private void broadcast(Component message) {
        MinecraftServer.getConnectionManager().getOnlinePlayers()
                .forEach(player -> player.sendMessage(message));
    }
}
