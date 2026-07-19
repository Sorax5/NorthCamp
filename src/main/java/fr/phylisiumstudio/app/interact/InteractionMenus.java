package fr.phylisiumstudio.app.interact;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.activity.ActivityType;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.slot.Slot;
import fr.phylisiumstudio.logic.slot.SlotService;
import fr.phylisiumstudio.logic.staff.Staff;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

/**
 * Construit les menus tchat focalisés ouverts au clic droit sur une cible
 * (emplacement, activité, employé, client, slot à acheter). Les boutons
 * réutilisent les commandes existantes.
 */
@Singleton
public class InteractionMenus {

    private final MarketService marketService;
    private final SlotService slotService;

    @Inject
    public InteractionMenus(MarketService marketService, SlotService slotService) {
        this.marketService = marketService;
        this.slotService = slotService;
    }

    /** Clé de position stable (au bloc) pour identifier un slot. */
    public static String slotKey(Vector3d position) {
        return (long) Math.floor(position.x) + ";" + (long) Math.floor(position.y)
                + ";" + (long) Math.floor(position.z);
    }

    public void openPlot(Player player, Campsite campsite, UUID plotId) {
        var plot = campsite.getPlots().stream().filter(p -> p.getUniqueID().equals(plotId))
                .findFirst().orElse(null);
        if (plot == null) return;

        var occupant = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING
                        && c.getPlot() != null && c.getPlot().getUniqueID().equals(plotId))
                .findFirst().orElse(null);

        var menu = ChatMenu.titled("Emplacement " + plot.getPlotType().name())
                .text("Niveau : " + plot.getLevel(), NamedTextColor.WHITE)
                .text("Prix : " + Math.round(plot.getPrice()) + " $  (marché "
                        + Math.round(marketService.fairPrice(plot.getPlotType())) + " $)", NamedTextColor.GRAY)
                .text("État : " + (plot.isDirty() ? "Sale" : "Propre"),
                        plot.isDirty() ? NamedTextColor.RED : NamedTextColor.GREEN);
        if (occupant != null) {
            menu.text("Occupé — " + occupant.getRemainingDays() + "j, satisf. "
                    + Math.round(occupant.getSatisfaction()) + "%", NamedTextColor.YELLOW);
        } else {
            menu.text("Libre", NamedTextColor.GRAY);
        }
        menu.blank().line(ChatMenu.button("Gérer les tarifs", NamedTextColor.YELLOW, "/pricing", "Tarification"))
                .footer().send(player);
    }

    public void openActivity(Player player, Campsite campsite, UUID activityId) {
        var activity = campsite.getActivities().stream().filter(a -> a.getUniqueID().equals(activityId))
                .findFirst().orElse(null);
        if (activity == null) return;

        var menu = ChatMenu.titled("Activité " + activity.getType().name())
                .text("Niveau : " + activity.getCurrentLevel(), NamedTextColor.WHITE)
                .text("Prix : " + Math.round(activity.getPrice()) + " $", NamedTextColor.GRAY)
                .text("État : " + (activity.isOperational() ? "Disponible" : "En panne"),
                        activity.isOperational() ? NamedTextColor.GREEN : NamedTextColor.RED)
                .text("Clients : " + activity.getCurrentClients().size() + "/" + activity.getMaxClients(),
                        NamedTextColor.GRAY)
                .blank()
                .line(ChatMenu.row(
                        ChatMenu.button("-1", NamedTextColor.RED, "/activities price " + activityId + " -1", "Baisser le prix"),
                        ChatMenu.button("+1", NamedTextColor.GREEN, "/activities price " + activityId + " 1", "Augmenter le prix"),
                        activity.isOperational()
                                ? Component.text("[OK]", NamedTextColor.DARK_GRAY)
                                : ChatMenu.button("Réparer", NamedTextColor.YELLOW, "/activities repair " + activityId, "Remettre en service")))
                .footer();
        menu.send(player);
    }

    public void openStaff(Player player, Campsite campsite, UUID staffId) {
        var staff = campsite.getStaff().stream().filter(s -> s.getUniqueId().equals(staffId))
                .findFirst().orElse(null);
        if (staff == null) return;

        var role = staff.getAssignedRole();
        ChatMenu.titled("Employé " + staff.getName())
                .text("Rôle : " + (role != null ? role.name() : "inactif"), NamedTextColor.AQUA)
                .text("Salaire : " + Math.round(staff.getDailySalary()) + " $/j", NamedTextColor.YELLOW)
                .blank()
                .line(ChatMenu.row(
                        ChatMenu.button("TP", NamedTextColor.GREEN, "/staff tp " + staffId, "Se téléporter"),
                        ChatMenu.button("Localiser", NamedTextColor.AQUA, "/staff locate " + staffId, "Le faire briller"),
                        ChatMenu.button("Gérer", NamedTextColor.YELLOW, "/staff", "Menu employés")))
                .footer().send(player);
    }

    public void openClient(Player player, Campsite campsite, UUID clientId) {
        var client = campsite.getClients().stream().filter(c -> c.getUniqueID().equals(clientId))
                .findFirst().orElse(null);
        if (client == null) return;

        var menu = ChatMenu.titled(client.isFamily() ? "Famille ×" + client.getGroupSize() : "Client")
                .text("État : " + client.getLifecycle(), NamedTextColor.AQUA)
                .text("Séjour : " + client.getRemainingDays() + "/" + client.getTotalStayDays() + " j", NamedTextColor.WHITE)
                .text("Budget : " + Math.round(client.getBudget()) + " $", NamedTextColor.GREEN)
                .text("Satisfaction : " + Math.round(client.getSatisfaction()) + "%", NamedTextColor.LIGHT_PURPLE)
                .blank();
        var locate = ChatMenu.button("Localiser", NamedTextColor.AQUA, "/clients locate " + clientId, "Le faire briller");
        if (client.getLifecycle() == ClientLifecycle.WAITING) {
            menu.line(ChatMenu.row(
                    ChatMenu.button("Affecter", NamedTextColor.GREEN, "/clients assign " + clientId, "Installer ce client"),
                    locate));
        } else {
            menu.line(locate);
        }
        menu.footer().send(player);
    }

    public void openSlot(Player player, Campsite campsite, boolean isPlotSlot, String posKey) {
        List<Slot> slots = isPlotSlot
                ? slotService.availablePlotSlots(campsite)
                : slotService.availableActivitySlots(campsite);

        // Retrouve l'index courant du slot à partir de sa position (robuste aux achats).
        int index = -1;
        for (var slot : slots) {
            if (slotKey(slot.position()).equals(posKey)) {
                index = slot.index();
                break;
            }
        }
        if (index < 0) {
            player.sendMessage(Component.text("Cet emplacement n'est plus disponible.", NamedTextColor.RED));
            return;
        }

        var menu = ChatMenu.titled(isPlotSlot ? "Emplacement à acheter" : "Activité à acheter");
        if (isPlotSlot) {
            menu.text("Coût : " + Math.round(SlotService.PLOT_SLOT_PRICE) + " $", NamedTextColor.GOLD).blank();
            var parts = new Component[PlotType.values().length];
            for (int i = 0; i < PlotType.values().length; i++) {
                var name = PlotType.values()[i].name();
                parts[i] = ChatMenu.button(name, NamedTextColor.YELLOW,
                        "/slots buyplot " + index + " " + name, "Définir en " + name);
            }
            menu.line(ChatMenu.row(parts));
        } else {
            menu.text("Coût : " + Math.round(SlotService.ACTIVITY_SLOT_PRICE) + " $", NamedTextColor.GOLD).blank();
            var parts = new Component[ActivityType.values().length];
            for (int i = 0; i < ActivityType.values().length; i++) {
                var name = ActivityType.values()[i].name();
                parts[i] = ChatMenu.button(name, NamedTextColor.YELLOW,
                        "/slots buyactivity " + index + " " + name, "Définir en " + name);
            }
            menu.line(ChatMenu.row(parts));
        }
        menu.footer().send(player);
    }
}
