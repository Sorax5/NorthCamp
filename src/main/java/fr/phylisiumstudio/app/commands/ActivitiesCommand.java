package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityUpgradeService;
import fr.phylisiumstudio.logic.service.ActivityDataService;
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
    private final ActivityUpgradeService upgradeService;
    private final ActivityDataService activityDataService;

    @Inject
    public ActivitiesCommand(CampsiteService campsiteService,
                             ActivityUpgradeService upgradeService,
                             ActivityDataService activityDataService) {
        super("activities");
        this.campsiteService = campsiteService;
        this.upgradeService = upgradeService;
        this.activityDataService = activityDataService;

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

        addSyntax((sender, ctx) -> withCampsiteActivity(sender, UUID.fromString(ctx.get(idArg)), (campsite, activity) -> {
            upgradeActivity(sender, campsite, activity);
            showMenu(sender);
        }), ArgumentType.Literal("upgrade"), idArg);
    }

    private void upgradeActivity(CommandSender sender, Campsite campsite, Activity activity) {
        var data = activityDataService.getActivityData(activity.getType());
        if (!upgradeService.canUpgrade(data, activity)) {
            sender.sendMessage(Component.text("Cette activité est déjà au niveau maximum.", NamedTextColor.YELLOW));
            return;
        }
        long cost = upgradeService.nextCost(data, activity);
        if (upgradeService.upgrade(campsite, data, activity)) {
            sender.sendMessage(Component.text("Activité améliorée au niveau " + activity.getCurrentLevel()
                    + " (capacité " + activity.getMaxClients() + ", " + Math.round(activity.getPrice())
                    + " $/passage) pour " + cost + " $.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Amélioration impossible (solde insuffisant : " + cost + " $ requis).",
                    NamedTextColor.RED));
        }
    }

    private void withCampsiteActivity(CommandSender sender, UUID activityId,
                                      java.util.function.BiConsumer<Campsite, Activity> action) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;
        campsite.getActivities().stream()
                .filter(a -> a.getUniqueID().equals(activityId))
                .findFirst()
                .ifPresent(activity -> action.accept(campsite, activity));
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
                            + " — " + Math.round(activity.getPrice()) + " $ — cap." + activity.getMaxClients(),
                            NamedTextColor.GRAY))
                    .append(status));

            var row = ChatMenu.row(
                    ChatMenu.button("-1", NamedTextColor.RED, "/activities price " + activity.getUniqueID() + " -1", "Baisser le prix"),
                    ChatMenu.button("+1", NamedTextColor.GREEN, "/activities price " + activity.getUniqueID() + " 1", "Augmenter le prix"),
                    activity.isOperational()
                            ? Component.text("[OK]", NamedTextColor.DARK_GRAY)
                            : ChatMenu.button("Réparer", NamedTextColor.YELLOW, "/activities repair " + activity.getUniqueID(), "Remettre en service"));

            var data = activityDataService.getActivityData(activity.getType());
            if (upgradeService.canUpgrade(data, activity)) {
                row = ChatMenu.row(row,
                        ChatMenu.button("Améliorer (" + upgradeService.nextCost(data, activity) + " $)", NamedTextColor.GOLD,
                                "/activities upgrade " + activity.getUniqueID(), "Monter d'un niveau : +capacité, +revenu"));
            }
            menu.line(row);
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }
}
