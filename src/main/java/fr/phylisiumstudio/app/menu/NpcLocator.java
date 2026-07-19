package fr.phylisiumstudio.app.menu;

import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.staff.StaffEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.TaskSchedule;

import java.util.Optional;
import java.util.UUID;

/**
 * Aide à repérer un NPC (client ou employé) dans l'instance du joueur :
 * téléportation vers lui ou mise en surbrillance temporaire.
 */
public final class NpcLocator {
    private NpcLocator() {
    }

    private static final int GLOW_SECONDS = 5;

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
        npc.setGlowing(true);
        MinecraftServer.getSchedulerManager()
                .buildTask(() -> npc.setGlowing(false))
                .delay(TaskSchedule.seconds(GLOW_SECONDS))
                .schedule();
        player.sendMessage(Component.text("NPC mis en surbrillance " + GLOW_SECONDS + "s.", NamedTextColor.AQUA));
    }
}
