package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.rating.RatingService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

/**
 * Sidebar (tableau de score) affichant en temps réel les statistiques clés du
 * camping du joueur : argent, campeurs, file d'attente, réputation, etc.
 */
public class SidebarView {

    private final Player player;
    private final Campsite campsite;
    private final Sidebar sidebar;
    private final Task updateTask;

    public SidebarView(Player player, Campsite campsite) {
        this.player = player;
        this.campsite = campsite;
        this.sidebar = new Sidebar(Component.text("⛺ North Camp", NamedTextColor.GOLD, TextDecoration.BOLD));

        // Les lignes de score élevé s'affichent en haut.
        sidebar.createLine(new Sidebar.ScoreboardLine("rating", Component.empty(), 7));
        sidebar.createLine(new Sidebar.ScoreboardLine("money", Component.empty(), 6));
        sidebar.createLine(new Sidebar.ScoreboardLine("reputation", Component.empty(), 5));
        sidebar.createLine(new Sidebar.ScoreboardLine("campers", Component.empty(), 4));
        sidebar.createLine(new Sidebar.ScoreboardLine("queue", Component.empty(), 3));
        sidebar.createLine(new Sidebar.ScoreboardLine("plots", Component.empty(), 2));
        sidebar.createLine(new Sidebar.ScoreboardLine("staff", Component.empty(), 1));

        update();
        sidebar.addViewer(player);

        this.updateTask = player.scheduler().submitTask(() -> {
            update();
            return TaskSchedule.seconds(1);
        });
    }

    private void update() {
        long campers = countLifecycle(ClientLifecycle.STAYING);
        long queue = countLifecycle(ClientLifecycle.WAITING);
        long occupied = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING && c.getPlot() != null)
                .count();

        line("rating", "Note", RatingService.render(RatingService.ratingOf(campsite)), NamedTextColor.GOLD);
        line("money", "Argent", Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN);
        line("reputation", "Réputation", Math.round(campsite.getReputation()) + " / 100", NamedTextColor.LIGHT_PURPLE);
        line("campers", "Campeurs", String.valueOf(campers), NamedTextColor.AQUA);
        line("queue", "File d'attente", String.valueOf(queue), NamedTextColor.YELLOW);
        line("plots", "Emplacements", occupied + " / " + campsite.getPlots().size(), NamedTextColor.WHITE);
        line("staff", "Employés", String.valueOf(campsite.getStaff().size()), NamedTextColor.WHITE);
    }

    private void line(String id, String label, String value, NamedTextColor valueColor) {
        sidebar.updateLineContent(id, Component.text()
                .append(Component.text(label + " : ", NamedTextColor.GRAY))
                .append(Component.text(value, valueColor))
                .build());
    }

    private long countLifecycle(ClientLifecycle lifecycle) {
        return campsite.getClients().stream().filter(c -> c.getLifecycle() == lifecycle).count();
    }

    public void dispose() {
        updateTask.cancel();
        sidebar.removeViewer(player);
    }
}
