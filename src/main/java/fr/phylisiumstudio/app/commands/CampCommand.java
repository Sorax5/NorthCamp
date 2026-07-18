package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;

/**
 * {@code /camp} : menu principal du camping. Vue d'ensemble et accès aux
 * différents écrans de gestion via boutons cliquables.
 */
public class CampCommand extends Command {

    private final CampsiteService campsiteService;

    @Inject
    public CampCommand(CampsiteService campsiteService) {
        super("camp", "menu");
        this.campsiteService = campsiteService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        long waiting = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.WAITING).count();
        long staying = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING).count();

        ChatMenu.titled("North Camp — Gestion")
                .text("Solde : " + Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN)
                .text("Réputation : " + Math.round(campsite.getReputation()) + " / 100", NamedTextColor.LIGHT_PURPLE)
                .text("Emplacements : " + campsite.getPlots().size()
                        + "  |  Activités : " + campsite.getActivities().size()
                        + "  |  Employés : " + campsite.getStaff().size(), NamedTextColor.GRAY)
                .text("File d'attente : " + waiting + "  |  En séjour : " + staying, NamedTextColor.AQUA)
                .blank()
                .line(ChatMenu.row(
                        ChatMenu.button("Accueil", NamedTextColor.YELLOW, "/clients", "Affecter les clients"),
                        ChatMenu.button("Tarifs", NamedTextColor.YELLOW, "/pricing", "Tarification des emplacements"),
                        ChatMenu.button("Activités", NamedTextColor.YELLOW, "/activities", "Gérer les activités")))
                .line(ChatMenu.row(
                        ChatMenu.button("Employés", NamedTextColor.YELLOW, "/staff", "Ressources humaines"),
                        ChatMenu.button("Emplacements", NamedTextColor.YELLOW, "/slots", "Acquérir des emplacements"),
                        ChatMenu.button("Classement", NamedTextColor.YELLOW, "/leaderboard", "Classement mondial")))
                .footer()
                .send(sender);
    }
}
