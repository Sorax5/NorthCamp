package fr.phylisiumstudio.app.vendor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.app.interact.InteractionTags;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.economy.EconomyService;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.slot.LayoutService;
import fr.phylisiumstudio.logic.vendor.Patent;
import fr.phylisiumstudio.logic.vendor.VendorEntity;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les marchands ambulants : au lever du jour, un marchand présent depuis la
 * veille repart, puis un nouveau peut apparaître à l'entrée (par chance) pour
 * vendre 1–2 brevets non encore possédés. Si le joueur n'achète pas, le marchand
 * repart — il faut alors de la chance pour qu'un autre revienne.
 */
@Singleton
public class VendorService {

    /** Probabilité qu'un marchand se présente un jour donné. */
    private static final double SPAWN_CHANCE = 0.25;
    /** Nombre maximal de brevets proposés par un marchand. */
    private static final int MAX_OFFER = 2;

    private final EconomyService economyService;
    private final LayoutService layoutService;
    private final Random random;

    private final ConcurrentHashMap<UUID, VendorEntity> vendors = new ConcurrentHashMap<>();

    @Inject
    public VendorService(EconomyService economyService, LayoutService layoutService, Random random) {
        this.economyService = economyService;
        this.layoutService = layoutService;
        this.random = random;

        var node = EventNode.all("vendors");
        node.addListener(PhaseChangeEvent.class, this::onPhaseChange);
        MinecraftServer.getGlobalEventHandler().addChild(node);
    }

    private void onPhaseChange(PhaseChangeEvent event) {
        if (event.phase() != GamePhase.DAY) {
            return;
        }
        var campsite = event.campsite();
        var id = campsite.getUniqueID();

        // Le marchand de la veille repart (durée de vie : un jour).
        var previous = vendors.remove(id);
        if (previous != null) {
            previous.despawn();
        }

        if (random.nextDouble() >= SPAWN_CHANCE) {
            return;
        }
        var offered = pickPatents(campsite);
        if (offered.isEmpty()) {
            return;
        }

        var vendor = new VendorEntity(offered);
        vendor.setTag(InteractionTags.KIND, InteractionTags.VENDOR);
        vendor.setTag(InteractionTags.ID, id.toString());
        vendor.setInstance(event.getInstance(), PositionMapper.toMinestomPos(layoutService.exitPosition()));
        vendors.put(id, vendor);
    }

    /** Brevets actuellement proposés au camping (vide si aucun marchand présent). */
    public List<Patent> offeredPatents(UUID campsiteId) {
        var vendor = vendors.get(campsiteId);
        return vendor == null ? List.of() : List.copyOf(vendor.offered());
    }

    /**
     * Achète un brevet auprès du marchand présent.
     *
     * @return {@code true} si l'achat a réussi (marchand présent le proposant,
     *         brevet non possédé, fonds suffisants).
     */
    public boolean buy(Campsite campsite, Patent patent) {
        var vendor = vendors.get(campsite.getUniqueID());
        if (vendor == null || !vendor.offered().contains(patent)) {
            return false;
        }
        if (campsite.hasPatent(patent) || campsite.getMoney() < patent.cost()) {
            return false;
        }
        economyService.charge(campsite, patent.cost());
        campsite.addPatent(patent);
        vendor.offered().remove(patent);
        if (vendor.offered().isEmpty()) {
            vendor.despawn();
            vendors.remove(campsite.getUniqueID());
        }
        return true;
    }

    /** Tire 1–2 brevets non encore possédés par le camping. */
    private List<Patent> pickPatents(Campsite campsite) {
        var pool = new ArrayList<Patent>();
        for (var patent : Patent.values()) {
            if (!campsite.hasPatent(patent)) {
                pool.add(patent);
            }
        }
        java.util.Collections.shuffle(pool, random);
        return new ArrayList<>(pool.subList(0, Math.min(MAX_OFFER, pool.size())));
    }
}
