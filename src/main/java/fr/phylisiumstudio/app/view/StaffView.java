package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.skin.SkinLibrary;
import fr.phylisiumstudio.logic.staff.Staff;
import fr.phylisiumstudio.logic.staff.StaffEntity;
import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.InstanceContainer;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vue des entités employés d'un camping. Aligne les NPC employés le long de la
 * zone d'accueil et se re-synchronise à chaque jour (embauches / départs).
 */
public class StaffView {

    @Getter
    private final Campsite campsite;
    private final InstanceContainer instance;
    private final Vector3d staffOrigin;
    private final SkinLibrary skinLibrary;
    private final Map<UUID, StaffEntity> entities = new HashMap<>();
    private final EventListener<PhaseChangeEvent> phaseListener;

    public StaffView(Campsite campsite, InstanceContainer instance, Vector3d staffOrigin, SkinLibrary skinLibrary) {
        this.campsite = campsite;
        this.instance = instance;
        this.staffOrigin = staffOrigin;
        this.skinLibrary = skinLibrary;

        sync();

        this.phaseListener = EventListener.of(PhaseChangeEvent.class, event -> {
            if (event.campsite().getUniqueID().equals(campsite.getUniqueID())) {
                sync();
            }
        });
        MinecraftServer.getGlobalEventHandler().addListener(phaseListener);
    }

    /** Aligne les entités employés sur l'effectif courant. */
    public synchronized void sync() {
        int index = 0;
        for (var staff : campsite.getStaff()) {
            var slot = index++;
            entities.computeIfAbsent(staff.getUniqueId(), _ -> spawn(staff, slot));
        }

        var present = campsite.getStaff().stream().map(Staff::getUniqueId)
                .collect(Collectors.toSet());
        entities.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) {
                return false;
            }
            despawn(entry.getValue());
            return true;
        });
    }

    /** Retrait thread-safe via l'API Acquirable (sync() peut tourner hors tick thread). */
    private static void despawn(StaffEntity entity) {
        entity.getAcquirable().sync(e -> e.remove());
    }

    private StaffEntity spawn(Staff staff, int index) {
        var skin = skinLibrary.staffSkin(staff.getLook()).orElse(null);
        var position = new Vector3d(staffOrigin).add(index * 2.0, 0, 0);
        var entity = new StaffEntity(staff, skin);
        entity.setInstance(instance, PositionMapper.toMinestomPos(position));
        return entity;
    }

    public synchronized void dispose() {
        MinecraftServer.getGlobalEventHandler().removeListener(phaseListener);
        for (var entity : entities.values()) {
            despawn(entity);
        }
        entities.clear();
    }
}
