package fr.phylisiumstudio.logic.client;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;

public class ClientEntity extends EntityCreature {


    public ClientEntity() {
        super(EntityType.MANNEQUIN);
        this.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1);

        this.setCustomNameVisible(true);
        this.setAutoViewEntities(true);
    }

    public void setCurrentAction(String state, String action) {
        var txt = Component.text()
                .append(Component.text(state,NamedTextColor.GREEN))
                .append(Component.text(" - "))
                .append(Component.text(action, NamedTextColor.YELLOW))
                .build();

        this.setCustomName(txt);
    }

    public void setSleeping() {
        this.setPose(EntityPose.SLEEPING);
    }

    public void setStanding() {
        this.setPose(EntityPose.STANDING);
    }

    public void setSitting() {
        this.setPose(EntityPose.SITTING);
    }
}
