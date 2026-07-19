package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.app.vendor.VendorService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.vendor.Patent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;

/**
 * {@code /patents} : brevets du camping. Vue d'ensemble des brevets possédés et
 * achat de ceux proposés par le marchand ambulant présent à l'entrée.
 */
public class PatentsCommand extends Command {

    private final CampsiteService campsiteService;
    private final VendorService vendorService;

    @Inject
    public PatentsCommand(CampsiteService campsiteService, VendorService vendorService) {
        super("patents");
        this.campsiteService = campsiteService;
        this.vendorService = vendorService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var patentArg = ArgumentType.Enum("patent", Patent.class);
        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            var patent = ctx.get(patentArg);
            if (vendorService.buy(campsite, patent)) {
                sender.sendMessage(Component.text("Brevet acquis : " + patent.displayName()
                        + " (" + patent.cost() + " $).", NamedTextColor.GREEN));
                if (sender instanceof Player p) {
                    fr.phylisiumstudio.logic.effect.Toasts.goal(p,
                            Component.text("Brevet : " + patent.displayName()), Material.PAPER);
                }
            } else {
                sender.sendMessage(Component.text("Achat impossible (marchand absent, déjà possédé, "
                        + "ou solde insuffisant).", NamedTextColor.RED));
            }
            showMenu(sender);
        }, ArgumentType.Literal("buy"), patentArg);
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var menu = ChatMenu.titled("Brevets")
                .text("Solde : " + Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN)
                .blank()
                .text("— Possédés —", NamedTextColor.GOLD);

        if (campsite.getPatents().isEmpty()) {
            menu.text("Aucun brevet.", NamedTextColor.GRAY);
        } else {
            for (var patent : campsite.getPatents()) {
                menu.text("✔ " + patent.displayName() + " — " + patent.description(), NamedTextColor.GREEN);
            }
        }

        var offered = vendorService.offeredPatents(campsite.getUniqueID());
        menu.blank().text("— Marchand présent —", NamedTextColor.GOLD);
        if (offered.isEmpty()) {
            menu.text("Aucun marchand à l'entrée pour l'instant. Reviens tenter ta chance !", NamedTextColor.GRAY);
        } else {
            for (var patent : offered) {
                menu.line(ChatMenu.row(
                        Component.text(patent.displayName() + " ", NamedTextColor.WHITE),
                        ChatMenu.button("Acheter (" + patent.cost() + " $)", NamedTextColor.GREEN,
                                "/patents buy " + patent.name(), patent.description())));
            }
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }
}
