package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.rating.RatingService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.advancements.Advancement;
import net.minestom.server.advancements.AdvancementRoot;
import net.minestom.server.advancements.AdvancementTab;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Onglet d'avancements servant de <b>guide d'objectifs</b> : un arbre qui explique
 * quoi faire, comment et pourquoi, et qui se coche à mesure que le joueur
 * progresse. Ouvre l'écran des progrès (touche L) pour suivre sa route.
 */
public class ObjectivesView {

    private static final String BACKGROUND = "minecraft:textures/gui/advancements/backgrounds/adventure.png";
    /** Rend chaque onglet unique (les ids d'onglet ne doivent pas entrer en collision entre sessions). */
    private static final AtomicInteger TAB_SEQ = new AtomicInteger();

    private final Player player;
    private final Campsite campsite;
    private final AdvancementTab tab;
    private final Task refreshTask;

    private final Advancement buyPlot;
    private final Advancement checkIn;
    private final Advancement activity;
    private final Advancement staff;
    private final Advancement amenity;
    private final Advancement upgrade;
    private final Advancement star3;
    private final Advancement star5;

    public ObjectivesView(Player player, Campsite campsite) {
        this.player = player;
        this.campsite = campsite;

        var root = new AdvancementRoot(
                Component.text("⛺ North Camp"),
                Component.text("Fais grossir ton camping : plus d'argent, meilleure note."),
                Material.OAK_SIGN, FrameType.TASK, 0f, 0f, BACKGROUND);

        var manager = MinecraftServer.getAdvancementManager();
        // ponytail: l'onglet subsiste dans le manager après déconnexion (pas d'API de retrait) ;
        //           négligeable à cette échelle, l'id unique évite toute collision au rejoin.
        this.tab = manager.createTab("nc-guide-" + player.getUuid() + "-" + TAB_SEQ.incrementAndGet(), root);

        buyPlot = child("buyplot", "Acheter un emplacement",
                "Ouvre /slots et achète un terrain : c'est là que dorment tes campeurs.",
                Material.GRASS_BLOCK, 1f, 0f, root);
        checkIn = child("checkin", "Accueillir un client",
                "Via /clients (ou un employé Accueil) : installe un campeur pour toucher un loyer.",
                Material.OAK_DOOR, 2f, -1f, buyPlot);
        activity = child("activity", "Construire une activité",
                "Pêche, baignade, barbecue : de quoi occuper — et faire payer — les campeurs.",
                Material.FISHING_ROD, 2f, 1f, buyPlot);
        staff = child("staff", "Embaucher un employé",
                "/staff : accueil, nettoyage, maintenance, finance. Ils travaillent à ta place.",
                Material.IRON_PICKAXE, 3f, -1f, checkIn);
        amenity = child("amenity", "Construire un service",
                "/amenities : sanitaires, épicerie… chaque service rend les campeurs plus heureux.",
                Material.BELL, 3f, 1f, activity);
        upgrade = child("upgrade", "Améliorer un emplacement",
                "Monte le niveau d'un terrain : plus de revenu par nuit.",
                Material.DIAMOND, 4f, 0f, staff);
        star3 = child("star3", "Atteindre 3 étoiles",
                "Bonne réputation + campeurs satisfaits. La note fait aussi grimper tes tarifs.",
                Material.GOLD_INGOT, 5f, 0f, upgrade);
        star5 = child("star5", "Atteindre 5 étoiles",
                "L'excellence : premium maximal sur les loyers et revenus.",
                Material.NETHER_STAR, 6f, 0f, star3);

        tab.addViewer(player);
        refresh();

        this.refreshTask = player.scheduler().submitTask(() -> {
            refresh();
            return TaskSchedule.seconds(5);
        });
    }

    private Advancement child(String id, String title, String desc, Material icon, float x, float y, Advancement parent) {
        var advancement = new Advancement(Component.text(title), Component.text(desc), icon, FrameType.TASK, x, y);
        tab.createAdvancement("nc:" + id, advancement, parent);
        return advancement;
    }

    /** Met à jour l'état « atteint » depuis l'état courant du camping. */
    private void refresh() {
        int stars = RatingService.ratingOf(campsite);
        set(buyPlot, !campsite.getPlots().isEmpty());
        set(checkIn, campsite.getClients().stream().anyMatch(c -> c.getLifecycle() == ClientLifecycle.STAYING));
        set(activity, !campsite.getActivities().isEmpty());
        set(staff, !campsite.getStaff().isEmpty());
        set(amenity, !campsite.getBuiltAmenities().isEmpty());
        set(upgrade, campsite.getPlots().stream().anyMatch(p -> p.getLevel() > 0));
        set(star3, stars >= 3);
        set(star5, stars >= RatingService.MAX_STARS);
    }

    /** N'envoie de mise à jour que si l'état change, pour éviter le spam de paquets. */
    private void set(Advancement advancement, boolean achieved) {
        if (advancement.isAchieved() != achieved) {
            advancement.setAchieved(achieved);
        }
    }

    public void dispose() {
        refreshTask.cancel();
        tab.removeViewer(player);
    }
}
