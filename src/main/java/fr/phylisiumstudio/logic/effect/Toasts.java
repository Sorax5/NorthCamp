package fr.phylisiumstudio.logic.effect;

import net.kyori.adventure.text.Component;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.advancements.Notification;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;

/**
 * Petites notifications « toast » (le popup d'avancement de Minecraft) pour
 * informer le joueur d'un événement marquant sans encombrer le tchat : palier de
 * note atteint, événement du jour, faillite, achat important…
 */
public final class Toasts {
    private Toasts() {
    }

    /** Toast neutre (cadre tâche). */
    public static void task(Player player, Component title, Material icon) {
        send(player, title, FrameType.TASK, icon);
    }

    /** Toast d'objectif atteint (cadre objectif, doré). */
    public static void goal(Player player, Component title, Material icon) {
        send(player, title, FrameType.GOAL, icon);
    }

    /** Toast d'alerte forte (cadre défi, violet). */
    public static void challenge(Player player, Component title, Material icon) {
        send(player, title, FrameType.CHALLENGE, icon);
    }

    public static void send(Player player, Component title, FrameType frame, Material icon) {
        if (player == null) {
            return;
        }
        player.sendNotification(new Notification(title, frame, icon));
    }
}
