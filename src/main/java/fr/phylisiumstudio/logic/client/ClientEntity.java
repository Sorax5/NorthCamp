package fr.phylisiumstudio.logic.client;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientEntity extends EntityCreature {
    private static final Logger logger = LoggerFactory.getLogger(ClientEntity.class);

    public ClientEntity() {
        super(EntityType.MANNEQUIN);

        AttributeInstance speed = this.getAttribute(Attribute.MOVEMENT_SPEED);
        speed.setBaseValue(0.1);

        this.setCustomNameVisible(true);
        this.setAutoViewEntities(true);
    }

    public void setCurrentAction(String state, String action) {
        try {
            var txt = Component.text()
                    .append(Component.text(state, NamedTextColor.GREEN))
                    .append(Component.text(" - "))
                    .append(Component.text(action, NamedTextColor.YELLOW))
                    .build();

            this.set(DataComponents.CUSTOM_NAME, txt);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour de l'action pour l'entité {}", getUuid(), e);
        }
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
