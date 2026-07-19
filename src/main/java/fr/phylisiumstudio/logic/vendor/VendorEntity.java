package fr.phylisiumstudio.logic.vendor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;

import java.util.List;

/**
 * NPC marchand ambulant, présent temporairement à l'entrée du camping. Il propose
 * quelques brevets à l'achat, puis repart. Entité passive (aucune IA) ; sa
 * matérialisation et son marquage interactif sont gérés par le service marchand.
 */
public class VendorEntity extends EntityCreature {

    private final List<Patent> offered;

    public VendorEntity(List<Patent> offered) {
        super(EntityType.MANNEQUIN);
        this.offered = offered;

        editEntityMeta(MannequinMeta.class, meta -> {
            meta.setNotifyAboutChanges(false);
            meta.setCustomNameVisible(true);
            meta.setNotifyAboutChanges(true);
        });
        this.set(DataComponents.CUSTOM_NAME,
                Component.text("Marchand ambulant", NamedTextColor.GOLD));
    }

    /** Brevets encore proposés (mutable : retirés au fur et à mesure des achats). */
    public List<Patent> offered() {
        return offered;
    }

    /** Retire le marchand du monde (thread-safe). */
    public void despawn() {
        getAcquirable().sync(Entity::remove);
    }
}
