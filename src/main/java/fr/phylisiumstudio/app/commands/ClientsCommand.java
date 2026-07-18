package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.CheckInService;
import fr.phylisiumstudio.logic.gameplay.PlotAssignmentService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.UUID;

/**
 * {@code /clients} : accueil et affectation. Liste les clients en attente et
 * permet de les installer sur un emplacement disponible d'un clic.
 */
public class ClientsCommand extends Command {

    private final CampsiteService campsiteService;
    private final CheckInService checkInService;
    private final PlotAssignmentService assignmentService;

    @Inject
    public ClientsCommand(CampsiteService campsiteService, CheckInService checkInService,
                          PlotAssignmentService assignmentService) {
        super("clients");
        this.campsiteService = campsiteService;
        this.checkInService = checkInService;
        this.assignmentService = assignmentService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var idArg = ArgumentType.Word("id");
        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            assignClient(sender, campsite, UUID.fromString(ctx.get(idArg)));
            showMenu(sender);
        }, ArgumentType.Literal("assign"), idArg);
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var waiting = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.WAITING).toList();
        var staying = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING).toList();

        var menu = ChatMenu.titled("Accueil des clients")
                .text("File d'attente : " + waiting.size()
                        + "  |  En séjour : " + staying.size(), NamedTextColor.AQUA)
                .blank();

        int available = assignmentService.availablePlots(campsite).size();
        if (waiting.isEmpty()) {
            menu.text("Personne à l'accueil.", NamedTextColor.GRAY);
        } else {
            menu.text("Emplacements libres : " + available, NamedTextColor.GRAY);
            for (var client : waiting) {
                menu.line(ChatMenu.row(describe(client), assignButton(campsite, client)));
            }
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }

    private Component describe(Client client) {
        var kind = client.isFamily() ? "Famille ×" + client.getGroupSize() : "Client seul";
        return Component.text(kind, NamedTextColor.WHITE)
                .append(Component.text(" — séjour " + client.getTotalStayDays() + "j, budget "
                        + Math.round(client.getBudget()) + " $, satisf. "
                        + Math.round(client.getSatisfaction()) + "%", NamedTextColor.GRAY));
    }

    private Component assignButton(Campsite campsite, Client client) {
        boolean canPlace = assignmentService.availablePlots(campsite).stream()
                .anyMatch(p -> !client.isFamily() || p.getLevel() >= PlotAssignmentService.FAMILY_MIN_LEVEL);
        if (!canPlace) {
            return Component.text("[Aucun emplacement adapté]", NamedTextColor.DARK_GRAY);
        }
        return ChatMenu.button("Affecter", NamedTextColor.GREEN,
                "/clients assign " + client.getUniqueID(), "Installer ce client");
    }

    private void assignClient(CommandSender sender, Campsite campsite, UUID clientId) {
        var client = campsite.getClients().stream()
                .filter(c -> c.getUniqueID().equals(clientId))
                .findFirst().orElse(null);
        if (client == null) return;

        var plot = assignmentService.availablePlots(campsite).stream()
                .filter(p -> !client.isFamily() || p.getLevel() >= PlotAssignmentService.FAMILY_MIN_LEVEL)
                .findFirst();
        if (plot.isEmpty()) {
            sender.sendMessage(Component.text("Aucun emplacement disponible pour ce client.", NamedTextColor.RED));
            return;
        }

        var outcome = checkInService.checkIn(campsite, client, plot.get());
        if (outcome.isSuccess()) {
            sender.sendMessage(Component.text("Client installé pour "
                    + Math.round(plot.get().getPrice()) + " $.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Échec de l'affectation : " + outcome, NamedTextColor.RED));
        }
    }
}
