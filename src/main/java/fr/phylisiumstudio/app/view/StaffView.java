package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.skin.SkinLibrary;
import fr.phylisiumstudio.logic.staff.Staff;
import fr.phylisiumstudio.logic.staff.StaffEntity;
import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.InstanceContainer;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Vue des entités employés d'un camping. Chaque employé est un NPC qui se dirige
 * physiquement vers son travail selon son rôle (emplacement sale à nettoyer,
 * activité en panne à réparer, sinon l'accueil). Re-synchronisée chaque jour.
 */
public class StaffView {

    @Getter
    private final Campsite campsite;
    private final InstanceContainer instance;
    private final Vector3d staffOrigin;
    private final Vector3d reception;
    private final SkinLibrary skinLibrary;
    private final Map<UUID, StaffEntity> entities = new HashMap<>();
    private final EventListener<PhaseChangeEvent> phaseListener;

    public StaffView(Campsite campsite, InstanceContainer instance, Vector3d staffOrigin,
                     Vector3d reception, SkinLibrary skinLibrary) {
        this.campsite = campsite;
        this.instance = instance;
        this.staffOrigin = staffOrigin;
        this.reception = reception;
        this.skinLibrary = skinLibrary;

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
        var entity = new StaffEntity(staff, skin, workTarget(staff));
        entity.setInstance(instance, PositionMapper.toMinestomPos(spawnPos));
        return entity;
    }

    /** Cible de travail courante de l'employé, réévaluée à chaque appel. */
    private Supplier<Pos> workTarget(Staff staff) {
        return () -> {
            var role = staff.getAssignedRole();
            Vector3d base;
            if (role == null) {
                base = reception;
            } else {
                base = switch (role) {
                    case CLEANING -> firstDirtyPlot().orElse(reception);
                    case MAINTENANCE -> firstBrokenActivity().orElse(reception);
                    default -> reception;
                };
            }
            return PositionMapper.toMinestomPos(base);
        };
    }

    private Optional<Vector3d> firstDirtyPlot() {
        return campsite.getPlots().stream()
                .filter(Plot::isDirty)
                .map(Plot::getPosition)
                .findFirst();
    }

    private Optional<Vector3d> firstBrokenActivity() {
        return campsite.getActivities().stream()
                .filter(a -> !a.isOperational())
                .map(Activity::getPosition)
                .findFirst();
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
