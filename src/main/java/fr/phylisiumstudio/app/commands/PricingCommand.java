package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.List;

/**
 * {@code /pricing} : menu de tarification dynamique. Le joueur ajuste le prix
 * de ses emplacements par type et le compare au prix juste du marché local.
 */
public class PricingCommand extends Command {

    private final CampsiteService campsiteService;
    private final MarketService marketService;

    @Inject
    public PricingCommand(CampsiteService campsiteService, MarketService marketService) {
        super("pricing");
        this.campsiteService = campsiteService;
        this.marketService = marketService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var typeArg = ArgumentType.Enum("type", PlotType.class);
        var amountArg = ArgumentType.Double("amount");

        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            setPrice(campsite, ctx.get(typeArg), ctx.get(amountArg));
            showMenu(sender);
        }, ArgumentType.Literal("set"), typeArg, amountArg);

        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            adjustPrice(campsite, ctx.get(typeArg), ctx.get(amountArg));
            showMenu(sender);
        }, ArgumentType.Literal("adjust"), typeArg, amountArg);
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var menu = ChatMenu.titled("Tarification");
        for (var type : PlotType.values()) {
            var plots = plotsOfType(campsite, type);
            if (plots.isEmpty()) continue;

            double current = plots.get(0).getPrice();
            double fair = marketService.fairPrice(type);
            var verdict = current > fair * 1.1
                    ? Component.text(" (trop cher !)", NamedTextColor.RED)
                    : Component.text(" (compétitif)", NamedTextColor.GREEN);

            menu.line(Component.text(type.displayName() + " ×" + plots.size(), NamedTextColor.AQUA)
                    .append(Component.text("  vous: " + Math.round(current) + " $ | marché: "
                            + Math.round(fair) + " $", NamedTextColor.GRAY))
                    .append(verdict));

            menu.line(ChatMenu.row(
                    ChatMenu.button("-10", NamedTextColor.RED, "/pricing adjust " + type.name() + " -10", "Baisser de 10"),
                    ChatMenu.button("-1", NamedTextColor.RED, "/pricing adjust " + type.name() + " -1", "Baisser de 1"),
                    ChatMenu.button("+1", NamedTextColor.GREEN, "/pricing adjust " + type.name() + " 1", "Augmenter de 1"),
                    ChatMenu.button("+10", NamedTextColor.GREEN, "/pricing adjust " + type.name() + " 10", "Augmenter de 10"),
                    ChatMenu.suggestButton("Définir", NamedTextColor.YELLOW, "/pricing set " + type.name() + " ", "Fixer un prix exact")));
        }
        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }

    private List<Plot> plotsOfType(Campsite campsite, PlotType type) {
        return campsite.getPlots().stream().filter(p -> p.getPlotType() == type).toList();
    }

    private void setPrice(Campsite campsite, PlotType type, double price) {
        double clamped = Math.max(0, price);
        plotsOfType(campsite, type).forEach(p -> p.setPrice(clamped));
    }

    private void adjustPrice(Campsite campsite, PlotType type, double delta) {
        plotsOfType(campsite, type).forEach(p -> p.setPrice(Math.max(0, p.getPrice() + delta)));
    }
}
