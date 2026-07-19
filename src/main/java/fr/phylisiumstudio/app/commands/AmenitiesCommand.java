package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.amenity.Amenity;
import fr.phylisiumstudio.logic.amenity.AmenityService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

/**
 * {@code /amenities} : construction des services du camping (sanitaires, épicerie,
 * Wi-Fi…). Chaque aménagement construit améliore le confort quotidien des campeurs.
 */
public class AmenitiesCommand extends Command {

    private final CampsiteService campsiteService;
    private final AmenityService amenityService;

    @Inject
    public AmenitiesCommand(CampsiteService campsiteService, AmenityService amenityService) {
        super("amenities");
        this.campsiteService = campsiteService;
        this.amenityService = amenityService;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var amenityArg = ArgumentType.Enum("amenity", Amenity.class);
        addSyntax((sender, ctx) -> {
            var campsite = CampsiteResolver.resolve(sender, campsiteService);
            if (campsite == null) return;
            var amenity = ctx.get(amenityArg);
            if (amenityService.build(campsite, amenity)) {
                sender.sendMessage(Component.text(amenity.displayName() + " construit(e) pour "
                        + amenity.cost() + " $.", NamedTextColor.GREEN));
                if (sender instanceof net.minestom.server.entity.Player p) {
                    fr.phylisiumstudio.logic.effect.Toasts.goal(p,
                            Component.text("Service ajouté : " + amenity.displayName()),
                            net.minestom.server.item.Material.BELL);
                }
            } else {
                sender.sendMessage(Component.text("Construction impossible (déjà présent ou solde insuffisant : "
                        + amenity.cost() + " $).", NamedTextColor.RED));
            }
            showMenu(sender);
        }, ArgumentType.Literal("build"), amenityArg);
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var menu = ChatMenu.titled("Aménagements")
                .text("Solde : " + Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN)
                .text("Confort/jour par campeur : +" + amenityService.dailyComfortBonus(campsite), NamedTextColor.AQUA)
                .blank()
                .text("— Construits —", NamedTextColor.GOLD);

        if (campsite.getAmenities().isEmpty()) {
            menu.text("Aucun aménagement.", NamedTextColor.GRAY);
        } else {
            for (var amenity : campsite.getAmenities()) {
                menu.text("✔ " + amenity.displayName(), NamedTextColor.GREEN);
            }
        }

        var buildable = amenityService.buildable(campsite);
        if (!buildable.isEmpty()) {
            menu.blank().text("— À construire —", NamedTextColor.GOLD);
            for (var amenity : buildable) {
                menu.line(ChatMenu.row(
                        Component.text(amenity.displayName() + " ", NamedTextColor.WHITE),
                        ChatMenu.button("Construire (" + amenity.cost() + " $)", NamedTextColor.YELLOW,
                                "/amenities build " + amenity.name(), "Construire " + amenity.displayName())));
            }
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }
}
