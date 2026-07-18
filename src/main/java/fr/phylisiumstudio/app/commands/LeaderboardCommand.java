package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.logic.leaderboard.LeaderboardMetric;
import fr.phylisiumstudio.logic.leaderboard.LeaderboardService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;

/**
 * {@code /leaderboard} : affiche le classement mondial (top 10 des revenus)
 * et, pour un joueur, son rang sur chaque statistique.
 */
public class LeaderboardCommand extends Command {

    private static final int TOP_LIMIT = 10;

    private final LeaderboardService leaderboardService;

    @Inject
    public LeaderboardCommand(LeaderboardService leaderboardService) {
        super("leaderboard", "lb");
        this.leaderboardService = leaderboardService;

        setDefaultExecutor(this::execute);
    }

    private void execute(CommandSender sender, CommandContext ctx) {
        sender.sendMessage(Component.text("=== Classement mondial : Revenus ===", NamedTextColor.GOLD));

        var top = leaderboardService.top(LeaderboardMetric.REVENUE, TOP_LIMIT);
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("Aucun camping classé pour l'instant.", NamedTextColor.GRAY));
            return;
        }
        for (var entry : top) {
            sender.sendMessage(Component.text(
                    "#" + entry.rank() + " — " + Math.round(entry.value()) + " $",
                    NamedTextColor.YELLOW));
        }

        if (sender instanceof Player player) {
            sender.sendMessage(Component.text("Vos rangs :", NamedTextColor.AQUA));
            for (var metric : LeaderboardMetric.values()) {
                var rank = leaderboardService.rankOf(player.getUuid(), metric);
                var text = rank.map(r -> "#" + r).orElse("non classé");
                sender.sendMessage(Component.text(" - " + metric.displayName() + " : " + text, NamedTextColor.WHITE));
            }
        }
    }
}
