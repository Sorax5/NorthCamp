package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivitySupplyService;
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
    private final ActivitySupplyService supplyService;

    @Inject
    public ActivitiesCommand(CampsiteService campsiteService,
                             ActivityUpgradeService upgradeService,
                             ActivityDataService activityDataService,
                             ActivitySupplyService supplyService) {
        super("activities");
        this.campsiteService = campsiteService;
        this.upgradeService = upgradeService;
        this.activityDataService = activityDataService;
        this.supplyService = supplyService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var idArg = ArgumentType.Word("id");
        var amountArg = ArgumentType.Double("amount");
        var restockArg = ArgumentType.Integer("amount");

        addSyntax((sender, ctx) -> withCampsiteActivity(sender, UUID.fromString(ctx.get(idArg)), (campsite, activity) -> {
            restock(sender, campsite, activity, ctx.get(restockArg));
            showMenu(sender);
        }), ArgumentType.Literal("restock"), idArg, restockArg);

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

    private void restock(CommandSender sender, Campsite campsite, Activity activity, int amount) {
        if (!activity.getType().consumesSupplies()) {
            sender.sendMessage(Component.text("Cette activité ne consomme pas de fournitures.", NamedTextColor.YELLOW));
            return;
        }
        long cost = supplyService.restockCost(activity, amount);
        if (supplyService.restock(campsite, activity, amount) > 0) {
            sender.sendMessage(Component.text("Ravitaillé de " + amount + " (stock " + activity.getSupplies()
                    + ") pour " + cost + " $.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Ravitaillement impossible (solde insuffisant : " + cost + " $).",
                    NamedTextColor.RED));
        }
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
            if (sender instanceof net.minestom.server.entity.Player p) {
                fr.phylisiumstudio.logic.effect.Toasts.task(p,
                        Component.text(activity.getType().displayName() + " niveau " + activity.getCurrentLevel()),
                        net.minestom.server.item.Material.FISHING_ROD);
            }
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
            var stockText = activity.getType().consumesSupplies()
                    ? " — stock " + activity.getSupplies()
                    : "";
            menu.line(Component.text(activity.getType().displayName(), NamedTextColor.AQUA)
                    .append(Component.text(" niv." + activity.getCurrentLevel()
                            + " — " + Math.round(activity.getPrice()) + " $ — cap." + activity.getMaxClients()
                            + stockText, NamedTextColor.GRAY))
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
            if (activity.getType().consumesSupplies()) {
                row = ChatMenu.row(row,
                        ChatMenu.button("Ravitailler +10 (" + supplyService.restockCost(activity, 10) + " $)",
                                NamedTextColor.YELLOW, "/activities restock " + activity.getUniqueID() + " 10",
                                "Acheter 10 fournitures"));
            }
            menu.line(row);
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }
}
