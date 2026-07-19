package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.app.interact.InteractionMenus;
import fr.phylisiumstudio.app.interact.InteractionTags;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.slot.Slot;
import fr.phylisiumstudio.logic.slot.SlotService;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Affiche un panneau « emplacement libre » et une hitbox cliquable au-dessus de
 * chaque slot constructible non acheté. Clic droit → menu d'achat.
 */
public class SlotView {

    private static final EntityType TEXT_DISPLAY = EntityType.fromKey("minecraft:text_display");
    private static final EntityType INTERACTION = EntityType.fromKey("minecraft:interaction");
    private static final Vector3d OFFSET = new Vector3d(0.5, 2.0, 0.5);
    private static final int REFRESH_SECONDS = 3;

    @Getter
    private final Campsite campsite;
    private final InstanceContainer instance;
    private final SlotService slotService;
    private final Map<String, List<Entity>> slots = new HashMap<>();
    private final Task refreshTask;

    public SlotView(Campsite campsite, InstanceContainer instance, SlotService slotService) {
        this.campsite = campsite;
        this.instance = instance;
        this.slotService = slotService;

        refresh();
        this.refreshTask = instance.scheduler().submitTask(() -> {
            refresh();
            return TaskSchedule.seconds(REFRESH_SECONDS);
        });
    }

    /** Aligne les panneaux de slots sur les emplacements encore libres. */
    public synchronized void refresh() {
        var present = new java.util.HashSet<String>();

        for (var slot : slotService.availablePlotSlots(campsite)) {
            show(slot, InteractionTags.SLOT_PLOT, "Emplacement de camping libre", present);
        }
        for (var slot : slotService.availableActivitySlots(campsite)) {
            show(slot, InteractionTags.SLOT_ACTIVITY, "Emplacement d'activité libre", present);
        }
        for (var slot : slotService.availableAmenitySlots(campsite)) {
            show(slot, InteractionTags.SLOT_AMENITY, "Emplacement de service libre", present);
        }

        // Retire les slots qui ne sont plus disponibles (achetés).
        slots.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().forEach(e -> e.getAcquirable().sync(Entity::remove));
            return true;
        });
    }

    private void show(Slot slot, String kind, String label, java.util.Set<String> present) {
        var key = InteractionMenus.slotKey(slot.position());
        present.add(key);
        slots.computeIfAbsent(key, _ -> spawn(slot, kind, label));
    }

    private List<Entity> spawn(Slot slot, String kind, String label) {
        var textPos = new Vector3d(slot.position()).add(OFFSET);

        var text = new Entity(TEXT_DISPLAY);
        text.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setSeeThrough(true);
            meta.setBackgroundColor(0x50003300);
            meta.setText(Component.text()
                    .append(Component.text(label, NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("Clic droit pour acheter", NamedTextColor.GRAY))
                    .build());
        });
        text.setInstance(instance, PositionMapper.toMinestomPos(textPos));

        var hitbox = new Entity(INTERACTION);
        hitbox.editEntityMeta(InteractionMeta.class, meta -> {
            meta.setWidth(1.4f);
            meta.setHeight(1.6f);
            meta.setResponse(true);
        });
        hitbox.setTag(InteractionTags.KIND, kind);
        hitbox.setTag(InteractionTags.ID, InteractionMenus.slotKey(slot.position()));
        hitbox.setInstance(instance, PositionMapper.toMinestomPos(new Vector3d(slot.position()).add(0.5, 0.5, 0.5)));

        var list = new ArrayList<Entity>();
        list.add(text);
        list.add(hitbox);
        return list;
    }

    public synchronized void dispose() {
        refreshTask.cancel();
        slots.values().forEach(list -> list.forEach(e -> e.getAcquirable().sync(Entity::remove)));
        slots.clear();
    }
}
