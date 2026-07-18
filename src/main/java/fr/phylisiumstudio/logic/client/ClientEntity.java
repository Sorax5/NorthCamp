package fr.phylisiumstudio.logic.client;

import com.badlogic.gdx.ai.btree.BehaviorTree;
import lombok.Data;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeInstance;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.network.player.ResolvableProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class ClientEntity extends EntityCreature {
    private static final Logger logger = LoggerFactory.getLogger(ClientEntity.class);

    private final ClientMemory memory;
    private final BehaviorTree<ClientEntity> behaviorTree;

    public ClientEntity(ClientMemory memory) {
        this(memory, null);
    }

    public ClientEntity(ClientMemory memory, PlayerSkin skin) {
        super(EntityType.MANNEQUIN);

        editEntityMeta(MannequinMeta.class, meta -> {
            meta.setNotifyAboutChanges(false);

            if (skin != null) {
                meta.setProfile(new ResolvableProfile(skin));
            }

            meta.setCustomNameVisible(true);

            meta.setNotifyAboutChanges(true);
        });

        AttributeInstance speed = this.getAttribute(Attribute.MOVEMENT_SPEED);
        speed.setBaseValue(0.1);

        this.setAutoViewEntities(true);

        this.memory = memory;
        this.behaviorTree = new BehaviorTree<>(new ClientRootNode(), this);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        behaviorTree.step();
    }

    public void setCurrentAction(String action) {
        var txt = Component.text()
                .append(Component.text(memory.getClient().getAction().toString(), NamedTextColor.GREEN))
                .append(Component.text(" - "))
                .append(Component.text(action, NamedTextColor.YELLOW))
                .build();

        this.set(DataComponents.CUSTOM_NAME, txt);
    }
}
