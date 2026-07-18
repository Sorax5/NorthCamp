package fr.phylisiumstudio.app.menu;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;

/**
 * Utilitaire partagé par les commandes de menu : résout le camping du joueur
 * émetteur, ou renvoie {@code null} après avoir envoyé le message d'erreur adéquat.
 */
public final class CampsiteResolver {

    private CampsiteResolver() {
    }

    public static Campsite resolve(CommandSender sender, CampsiteService campsiteService) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Commande réservée aux joueurs.", NamedTextColor.RED));
            return null;
        }
        var campsite = campsiteService.getCampsiteByOwner(player.getUuid());
        if (campsite.isEmpty()) {
            sender.sendMessage(Component.text("Vous n'avez pas de camping.", NamedTextColor.RED));
            return null;
        }
        return campsite.get();
    }
}
