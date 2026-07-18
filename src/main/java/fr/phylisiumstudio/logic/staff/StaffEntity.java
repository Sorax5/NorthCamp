package fr.phylisiumstudio.logic.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.network.player.ResolvableProfile;

import java.util.function.Supplier;

/**
 * NPC représentant un employé. Se déplace physiquement vers un point de travail
 * fourni par un {@link Supplier} (emplacement sale à nettoyer, activité à
 * réparer, accueil…), réévalué régulièrement pour suivre l'état du camping.
 */
public class StaffEntity extends EntityCreature {

    /** Réévaluation de la cible toutes les N ticks (~1 s). */
    private static final int RETARGET_INTERVAL = 20;
    private static final double ARRIVAL_DISTANCE = 1.5;

    private final Supplier<Pos> targetSupplier;
    private long tickCounter;

    public StaffEntity(Staff staff, PlayerSkin skin, Supplier<Pos> targetSupplier) {
        super(EntityType.MANNEQUIN);
        this.targetSupplier = targetSupplier;

        editEntityMeta(MannequinMeta.class, meta -> {
            meta.setNotifyAboutChanges(false);
            if (skin != null) {
                meta.setProfile(new ResolvableProfile(skin));
            }
            meta.setCustomNameVisible(true);
            meta.setNotifyAboutChanges(true);
        });

        getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);

        var role = staff.getAssignedRole();
        var label = Component.text()
                .append(Component.text(staff.getName(), NamedTextColor.GOLD))
                .append(Component.text(" — " + (role != null ? role.name() : "inactif"), NamedTextColor.GRAY))
                .build();
        this.set(DataComponents.CUSTOM_NAME, label);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (tickCounter++ % RETARGET_INTERVAL != 0) {
            return;
        }
        var target = targetSupplier.get();
        if (target == null) {
            return;
        }
        // Ne recalcule que si on n'est pas déjà quasiment arrivé, pour éviter de
        // spammer le pathfinder une fois sur place.
        if (getPosition().distance(target) > ARRIVAL_DISTANCE) {
            getNavigator().setPathTo(target);
        }
    }
}
