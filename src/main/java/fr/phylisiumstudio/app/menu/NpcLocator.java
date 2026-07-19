package fr.phylisiumstudio.app.menu;

import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.staff.StaffEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aide à repérer un NPC (client ou employé) dans l'instance du joueur :
 * téléportation vers lui ou mise en surbrillance temporaire.
 */
public final class NpcLocator {
    private NpcLocator() {
    }

    private static final int GLOW_SECONDS = 5;

    /** Tâche d'extinction en cours par NPC, pour qu'un re-locate ne l'éteigne pas trop tôt. */
    private static final Map<UUID, Task> glowTasks = new ConcurrentHashMap<>();

    public static Optional<Entity> findStaff(Player player, UUID staffId) {
        return find(player, e -> e instanceof StaffEntity se && se.staffId().equals(staffId));
    }

    public static Optional<Entity> findClient(Player player, UUID clientId) {
        return find(player, e -> e instanceof ClientEntity ce
                && ce.getMemory().getClient().getUniqueID().equals(clientId));
    }

    private static Optional<Entity> find(Player player, java.util.function.Predicate<Entity> match) {
        if (player.getInstance() == null) {
            return Optional.empty();
        }
        return player.getInstance().getEntities().stream().filter(match).findFirst();
    }

    /** Téléporte le joueur sur le NPC. */
    public static void teleportTo(Player player, Entity npc) {
        player.teleport(npc.getPosition());
        player.sendMessage(Component.text("Téléporté sur place.", NamedTextColor.GREEN));
    }

    /** Fait briller le NPC quelques secondes pour le repérer. */
    public static void highlight(Player player, Entity npc) {
        // Un re-locate annule l'extinction précédente pour repartir sur 5s pleins.
        var previous = glowTasks.remove(npc.getUuid());
        if (previous != null) {
            previous.cancel();
        }
        npc.setGlowing(true);
        var task = MinecraftServer.getSchedulerManager()
                .buildTask(() -> {
                    npc.setGlowing(false);
                    glowTasks.remove(npc.getUuid());
                })
                .delay(TaskSchedule.seconds(GLOW_SECONDS))
                .schedule();
        glowTasks.put(npc.getUuid(), task);
        player.sendMessage(Component.text("NPC mis en surbrillance " + GLOW_SECONDS + "s.", NamedTextColor.AQUA));
    }
}
