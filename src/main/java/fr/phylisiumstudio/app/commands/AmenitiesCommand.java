package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.amenity.AmenityService;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;

/**
 * {@code /amenities} : vue d'ensemble des services du camping (sanitaires,
 * épicerie, Wi-Fi…). La construction se fait en cliquant un emplacement de
 * service dans le camping ; chaque service construit améliore le confort
 * quotidien des campeurs.
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
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var menu = ChatMenu.titled("Services")
                .text("Solde : " + Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN)
                .text("Confort/jour par campeur : +" + amenityService.dailyComfortBonus(campsite), NamedTextColor.AQUA)
                .blank()
                .text("— Construits —", NamedTextColor.GOLD);

        if (campsite.getBuiltAmenities().isEmpty()) {
            menu.text("Aucun service.", NamedTextColor.GRAY);
        } else {
            for (var amenity : campsite.getBuiltAmenities()) {
                menu.text("✔ " + amenity.type().displayName(), NamedTextColor.GREEN);
            }
        }

        var buildable = amenityService.buildable(campsite);
        if (!buildable.isEmpty()) {
            menu.blank().text("— À construire —", NamedTextColor.GOLD);
            for (var amenity : buildable) {
                menu.text("• " + amenity.displayName() + " (" + amenity.cost() + " $)", NamedTextColor.WHITE);
            }
            menu.blank().text("Clique un emplacement de service (panneau vert) dans le camping pour bâtir.",
                    NamedTextColor.GRAY);
        }

        menu.footer();
        menu.line(ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal"));
        menu.send(sender);
    }
}
