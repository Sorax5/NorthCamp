package fr.phylisiumstudio.logic.staff;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.network.player.ResolvableProfile;

/**
 * NPC représentant un employé sur la carte. Stationnaire (le travail est
 * automatisé côté domaine) et affiche son nom et son rôle au-dessus de la tête.
 */
public class StaffEntity extends EntityCreature {

    public StaffEntity(Staff staff, PlayerSkin skin) {
        super(EntityType.MANNEQUIN);

        editEntityMeta(MannequinMeta.class, meta -> {
            meta.setNotifyAboutChanges(false);
            if (skin != null) {
                meta.setProfile(new ResolvableProfile(skin));
            }
            meta.setCustomNameVisible(true);
            meta.setNotifyAboutChanges(true);
        });

        var role = staff.getAssignedRole();
        var label = Component.text()
                .append(Component.text(staff.getName(), NamedTextColor.GOLD))
                .append(Component.text(" — " + (role != null ? role.name() : "inactif"), NamedTextColor.GRAY))
                .build();
        this.set(DataComponents.CUSTOM_NAME, label);
    }
}
