package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * {@code /activities} : gestion des activités. Prix, disponibilité et remise en
 * service manuelle des activités tombées en panne.
 */
public class ActivitiesCommand extends Command {

    private final CampsiteService campsiteService;

    @Inject
    public ActivitiesCommand(CampsiteService campsiteService) {
        super("activities");
        this.campsiteService = campsiteService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var idArg = ArgumentType.Word("id");
        var amountArg = ArgumentType.Double("amount");

        addSyntax((sender, ctx) -> withActivity(sender, UUID.fromString(ctx.get(idArg)), activity -> {
            activity.setOperational(true);
            showMenu(sender);
        }), ArgumentType.Literal("repair"), idArg);

        addSyntax((sender, ctx) -> withActivity(sender, UUID.fromString(ctx.get(idArg)), activity -> {
            activity.setPrice(Math.max(0, activity.getPrice() + ctx.get(amountArg)));
            showMenu(sender);
        }), ArgumentType.Literal("price"), idArg, amountArg);
    }

    private void withActivity(CommandSender sender, UUID activityId, Consumer<Activity> action) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;
        campsite.getActivities().stream()
                .filter(a -> a.getUniqueID().equals(activityId))
                .findFirst()
                .ifPresent(action);
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var menu = ChatMenu.titled("Activités");
        if (campsite.getActivities().isEmpty()) {
            menu.text("Aucune activité.", NamedTextColor.GRAY);
        }
        for (var activity : campsite.getActivities()) {
            var status = activity.isOperational()
                    ? Component.text(" ● disponible", NamedTextColor.GREEN)
                    : Component.text(" ● en panne", NamedTextColor.RED);
            menu.line(Component.text(activity.getType().name(), NamedTextColor.AQUA)
                    .append(Component.text(" niv." + activity.getCurrentLevel()
                            + " — " + Math.round(activity.getPrice()) + " $", NamedTextColor.GRAY))
                    .append(status));

            var row = ChatMenu.row(
                    ChatMenu.button("-1", NamedTextColor.RED, "/activities price " + activity.getUniqueID() + " -1", "Baisser le prix"),
                    ChatMenu.button("+1", NamedTextColor.GREEN, "/activities price " + activity.getUniqueID() + " 1", "Augmenter le prix"),
                    activity.isOperational()
                            ? Component.text("[OK]", NamedTextColor.DARK_GRAY)
                            : ChatMenu.button("Réparer", NamedTextColor.YELLOW, "/activities repair " + activity.getUniqueID(), "Remettre en service"));
            menu.line(row);
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }
}
