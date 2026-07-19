package fr.phylisiumstudio.app.interact;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerEntityInteractEvent;

import java.util.UUID;

/**
 * Ouvre le menu approprié quand un joueur fait clic droit sur une entité taguée
 * (NPC, panneau d'info, hitbox de slot). Point d'entrée unique (pattern Observer).
 */
@Singleton
public class InteractionService {

    private final CampsiteService campsiteService;
    private final InteractionMenus menus;

    @Inject
    public InteractionService(CampsiteService campsiteService, InteractionMenus menus) {
        this.campsiteService = campsiteService;
        this.menus = menus;

        var node = EventNode.all("interactions");
        node.addListener(PlayerEntityInteractEvent.class, this::onInteract);
        MinecraftServer.getGlobalEventHandler().addChild(node);
    }

    private void onInteract(PlayerEntityInteractEvent event) {
        // Une seule main pour éviter le double déclenchement.
        if (event.getHand() != PlayerHand.MAIN) {
            return;
        }
        var target = event.getTarget();
        var kind = target.getTag(InteractionTags.KIND);
        if (kind == null) {
            return;
        }

        var player = event.getPlayer();
        var campsite = campsiteService.getCampsiteByOwner(player.getUuid()).orElse(null);
        if (campsite == null) {
            return;
        }
        var id = target.getTag(InteractionTags.ID);
        // Entité taguée KIND sans ID : cible incohérente, on ignore le clic.
        if (id == null) {
            return;
        }

        dispatch(player, campsite, kind, id);
    }

    private void dispatch(Player player, Campsite campsite, String kind, String id) {
        switch (kind) {
            case InteractionTags.PLOT -> withUuid(id, u -> menus.openPlot(player, campsite, u));
            case InteractionTags.ACTIVITY -> withUuid(id, u -> menus.openActivity(player, campsite, u));
            case InteractionTags.STAFF -> withUuid(id, u -> menus.openStaff(player, campsite, u));
            case InteractionTags.CLIENT -> withUuid(id, u -> menus.openClient(player, campsite, u));
            case InteractionTags.SLOT_PLOT -> menus.openSlot(player, campsite, true, id);
            case InteractionTags.SLOT_ACTIVITY -> menus.openSlot(player, campsite, false, id);
            default -> { /* type inconnu : rien */ }
        }
    }

    /** Parse l'UUID sans jamais laisser une saisie corrompue casser le listener. */
    private void withUuid(String id, java.util.function.Consumer<UUID> action) {
        try {
            action.accept(UUID.fromString(id));
        } catch (IllegalArgumentException ignored) {
            // Tag ID malformé : cible invalide, rien à ouvrir.
        }
    }
}
