package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.marker.MarkerRegistry;
import fr.phylisiumstudio.logic.marker.MarkerTags;
import fr.phylisiumstudio.logic.plot.Plot;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Panneaux d'information (text displays) au-dessus de chaque emplacement et
 * activité, reflétant leur état interne (niveau, prix, propreté/panne,
 * occupation…). Rafraîchis périodiquement pour rester à jour.
 *
 * <p>Position : marqueur {@code info} du schématic s'il existe, sinon la position
 * de base du lieu, relevée pour flotter au-dessus.
 */
public class PlaceInfoView {

    private static final EntityType TEXT_DISPLAY = EntityType.fromKey("minecraft:text_display");
    private static final Vector3d DEFAULT_OFFSET = new Vector3d(0.5, 2.5, 0.5);
    private static final int REFRESH_SECONDS = 2;

    @Getter
    private final Campsite campsite;
    private final InstanceContainer instance;
    private final MarkerRegistry markerRegistry;
    private final Map<UUID, Entity> displays = new HashMap<>();
    private final Task refreshTask;

    public PlaceInfoView(Campsite campsite, InstanceContainer instance, MarkerRegistry markerRegistry) {
        this.campsite = campsite;
        this.instance = instance;
        this.markerRegistry = markerRegistry;

        refresh();
        this.refreshTask = instance.scheduler().submitTask(() -> {
            refresh();
            return TaskSchedule.seconds(REFRESH_SECONDS);
        });
    }

    /** Crée les panneaux manquants et met à jour tous les textes. */
    public synchronized void refresh() {
        for (var plot : campsite.getPlots()) {
            var display = displays.computeIfAbsent(plot.getUniqueID(),
                    _ -> spawn(plot.getUniqueID(), plot.getPosition()));
            setText(display, plotInfo(plot));
        }
        for (var activity : campsite.getActivities()) {
            var display = displays.computeIfAbsent(activity.getUniqueID(),
                    _ -> spawn(activity.getUniqueID(), activity.getPosition()));
            setText(display, activityInfo(activity));
        }
    }

    private Entity spawn(UUID placeId, Vector3d base) {
        var position = markerRegistry.get(placeId).firstOr(MarkerTags.INFO,
                new Vector3d(base).add(DEFAULT_OFFSET));

        var entity = new Entity(TEXT_DISPLAY);
        entity.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setSeeThrough(true);
            meta.setBackgroundColor(0x50000000); // fond noir semi-transparent
        });
        entity.setInstance(instance, PositionMapper.toMinestomPos(position));
        return entity;
    }

    private void setText(Entity entity, Component text) {
        entity.editEntityMeta(TextDisplayMeta.class, meta -> meta.setText(text));
    }

    private Component plotInfo(Plot plot) {
        var occupant = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING && plot.equals(c.getPlot()))
                .findFirst().orElse(null);

        var state = plot.isDirty()
                ? Component.text("Sale", NamedTextColor.RED)
                : Component.text("Propre", NamedTextColor.GREEN);
        var occupancy = occupant != null
                ? Component.text("Occupé — satisf. " + Math.round(occupant.getSatisfaction()) + "%", NamedTextColor.YELLOW)
                : Component.text("Libre", NamedTextColor.GRAY);

        return lines(
                Component.text(plot.getPlotType().name() + " niv." + plot.getLevel(), NamedTextColor.AQUA),
                Component.text("Prix : " + Math.round(plot.getPrice()) + " $", NamedTextColor.WHITE),
                state,
                occupancy);
    }

    private Component activityInfo(Activity activity) {
        var state = activity.isOperational()
                ? Component.text("Disponible", NamedTextColor.GREEN)
                : Component.text("En panne", NamedTextColor.RED);

        return lines(
                Component.text(activity.getType().name() + " niv." + activity.getCurrentLevel(), NamedTextColor.AQUA),
                Component.text("Prix : " + Math.round(activity.getPrice()) + " $", NamedTextColor.WHITE),
                state,
                Component.text("Clients : " + activity.getCurrentClients().size() + "/" + activity.getMaxClients(),
                        NamedTextColor.GRAY));
    }

    private Component lines(Component... lines) {
        var builder = Component.text();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append(Component.newline());
            }
            builder.append(lines[i]);
        }
        return builder.build();
    }

    public synchronized void dispose() {
        refreshTask.cancel();
        for (var entity : displays.values()) {
            entity.getAcquirable().sync(Entity::remove);
        }
        displays.clear();
    }
}
