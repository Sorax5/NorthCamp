package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.slot.Slot;
import fr.phylisiumstudio.logic.slot.SlotService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.List;

/**
 * {@code /slots} : achat et définition des emplacements constructibles découverts
 * sur la carte. Le joueur achète un slot libre puis choisit son type.
 */
public class SlotsCommand extends Command {

    private final CampsiteService campsiteService;
    private final SlotService slotService;

    @Inject
    public SlotsCommand(CampsiteService campsiteService, SlotService slotService) {
        super("slots");
        this.campsiteService = campsiteService;
        this.slotService = slotService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var indexArg = ArgumentType.Integer("index");
        var plotTypeArg = ArgumentType.Enum("type", PlotType.class);
        var activityTypeArg = ArgumentType.Enum("type", ActivityType.class);

        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            buyPlot(sender, campsite, ctx.get(indexArg), ctx.get(plotTypeArg));
            showMenu(sender);
        }, ArgumentType.Literal("buyplot"), indexArg, plotTypeArg);

        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            buyActivity(sender, campsite, ctx.get(indexArg), ctx.get(activityTypeArg));
            showMenu(sender);
        }, ArgumentType.Literal("buyactivity"), indexArg, activityTypeArg);
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var plotSlots = slotService.availablePlotSlots(campsite);
        var activitySlots = slotService.availableActivitySlots(campsite);

        var menu = ChatMenu.titled("Emplacements à acquérir")
                .text("Solde : " + Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN)
                .blank()
                .text("— Campings libres (" + Math.round(SlotService.PLOT_SLOT_PRICE) + " $) —", NamedTextColor.GOLD);

        if (plotSlots.isEmpty()) {
            menu.text("Aucun emplacement de camping libre.", NamedTextColor.GRAY);
        } else {
            for (var slot : plotSlots) {
                menu.line(ChatMenu.row(describe(slot), typeButtons(slot, "buyplot", PlotType.values())));
            }
        }

        menu.blank().text("— Activités libres (" + Math.round(SlotService.ACTIVITY_SLOT_PRICE) + " $) —", NamedTextColor.GOLD);
        if (activitySlots.isEmpty()) {
            menu.text("Aucun emplacement d'activité libre.", NamedTextColor.GRAY);
        } else {
            for (var slot : activitySlots) {
                menu.line(ChatMenu.row(describe(slot), typeButtons(slot, "buyactivity", ActivityType.values())));
            }
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }

    private Component describe(Slot slot) {
        var p = slot.position();
        return Component.text("#" + slot.index() + " (" + Math.round(p.x) + ", "
                + Math.round(p.y) + ", " + Math.round(p.z) + ")", NamedTextColor.AQUA);
    }

    private Component typeButtons(Slot slot, String verb, Enum<?>[] types) {
        var parts = new Component[types.length];
        for (int i = 0; i < types.length; i++) {
            var name = types[i].name();
            parts[i] = ChatMenu.button(name, NamedTextColor.YELLOW,
                    "/slots " + verb + " " + slot.index() + " " + name, "Définir en " + name);
        }
        return ChatMenu.row(parts);
    }

    private void buyPlot(CommandSender sender, Campsite campsite, int index, PlotType type) {
        var slots = slotService.availablePlotSlots(campsite);
        if (index < 0 || index >= slots.size()) {
            sender.sendMessage(Component.text("Emplacement introuvable.", NamedTextColor.RED));
            return;
        }
        boolean ok = slotService.buyPlot(campsite, slots.get(index).position(), type);
        feedback(sender, ok, "Camping " + type.name() + " acquis.");
    }

    private void buyActivity(CommandSender sender, Campsite campsite, int index, ActivityType type) {
        var slots = slotService.availableActivitySlots(campsite);
        if (index < 0 || index >= slots.size()) {
            sender.sendMessage(Component.text("Emplacement introuvable.", NamedTextColor.RED));
            return;
        }
        boolean ok = slotService.buyActivity(campsite, slots.get(index).position(), type);
        feedback(sender, ok, "Activité " + type.name() + " acquise.");
    }

    private void feedback(CommandSender sender, boolean ok, String success) {
        if (ok) {
            sender.sendMessage(Component.text(success + " (visible à la prochaine ouverture de l'instance)", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Achat impossible (solde insuffisant ou emplacement pris).", NamedTextColor.RED));
        }
    }
}
