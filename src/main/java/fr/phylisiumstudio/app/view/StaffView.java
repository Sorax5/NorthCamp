package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.app.interact.InteractionTags;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.skin.SkinLibrary;
import fr.phylisiumstudio.logic.staff.Staff;
import fr.phylisiumstudio.logic.staff.StaffBrain;
import fr.phylisiumstudio.logic.staff.StaffEntity;
import lombok.Getter;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.InstanceContainer;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vue des entités employés d'un camping. Chaque employé est un NPC qui, comme un
 * client, se rend physiquement sur sa tâche et l'accomplit ({@link StaffBrain}).
 * Re-synchronisée chaque jour (embauches / départs).
 */
public class StaffView {

    @Getter
    private final Campsite campsite;
    private final InstanceContainer instance;
    private final Vector3d staffOrigin;
    private final Vector3d reception;
    private final SkinLibrary skinLibrary;
    private final StaffBrain brain;
    private final Map<UUID, StaffEntity> entities = new HashMap<>();
    private final EventListener<PhaseChangeEvent> phaseListener;

    public StaffView(Campsite campsite, InstanceContainer instance, Vector3d staffOrigin,
                     Vector3d reception, SkinLibrary skinLibrary, StaffBrain brain) {
        this.campsite = campsite;
        this.instance = instance;
        this.staffOrigin = staffOrigin;
        this.reception = reception;
        this.skinLibrary = skinLibrary;
        this.brain = brain;

        sync();

        this.phaseListener = EventListener.of(PhaseChangeEvent.class, event -> sync());
        instance.eventNode().addListener(phaseListener);
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

    private StaffEntity spawn(Staff staff, int index) {
        var skin = skinLibrary.staffSkin(staff.getLook()).orElse(null);
        var spawnPos = new Vector3d(staffOrigin).add(index * 2.0, 0, 0);
        var entity = new StaffEntity(staff, skin, campsite, brain, reception);
        entity.setTag(InteractionTags.KIND, InteractionTags.STAFF);
        entity.setTag(InteractionTags.ID, staff.getUniqueId().toString());
        entity.setInstance(instance, PositionMapper.toMinestomPos(spawnPos));
        return entity;
    }

    private static void despawn(StaffEntity entity) {
        entity.getAcquirable().sync(e -> e.remove());
    }

    public synchronized void dispose() {
        instance.eventNode().removeListener(phaseListener);
        for (var entity : entities.values()) {
            despawn(entity);
        }
        entities.clear();
    }
}
