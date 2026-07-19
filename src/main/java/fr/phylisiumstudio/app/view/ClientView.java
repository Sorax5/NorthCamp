package fr.phylisiumstudio.app.view;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientEntity;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.client.ClientMemory;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.skin.SkinLibrary;
import lombok.Getter;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vue responsable des entités NPC d'un camping. Synchronise dynamiquement les
 * entités avec la liste de clients : les nouveaux arrivants apparaissent, les
 * clients partis disparaissent, au fil de la boucle de gameplay.
 */
public class ClientView {
    private static final Logger logger = LoggerFactory.getLogger(ClientView.class);

    @Getter
    private final Campsite campsite;
    private final InstanceContainer instance;
    private final Vector3d receptionPoint;
    private final Vector3d exitPoint;
    private final SkinLibrary skinLibrary;
    private final Random random;
    private final Map<UUID, ClientEntity> entities = new HashMap<>();
    private final EventListener<PhaseChangeEvent> phaseListener;
    private final Task syncTask;

    public ClientView(Campsite campsite, InstanceContainer instance, Vector3d receptionPoint,
                      Vector3d exitPoint, SkinLibrary skinLibrary, Random random) {
        this.campsite = campsite;
        this.instance = instance;
        this.receptionPoint = receptionPoint;
        this.exitPoint = exitPoint;
        this.skinLibrary = skinLibrary;
        this.random = random;

        sync();

        // Abonné au nœud d'événements de l'instance : ne reçoit que les transitions
        // de ce camping (PhaseChangeEvent est un InstanceEvent), nettoyé avec l'instance.
        this.phaseListener = EventListener.of(PhaseChangeEvent.class, event -> sync());
        instance.eventNode().addListener(phaseListener);

        // Synchronisation périodique : matérialise les arrivées progressives et
        // retire les partis sans attendre le changement de jour.
        this.syncTask = instance.scheduler().submitTask(() -> {
            sync();
            return TaskSchedule.seconds(2);
        });
    }

    /** Aligne les entités présentes sur l'état courant de la liste de clients. */
    public synchronized void sync() {
        // Spawn des clients présents mais pas encore matérialisés.
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() == ClientLifecycle.GONE) {
                continue;
            }
            entities.computeIfAbsent(client.getUniqueID(), _ -> spawn(client));
        }

        // Despawn des clients disparus de la liste ou repartis.
        var present = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() != ClientLifecycle.GONE)
                .map(Client::getUniqueID)
                .collect(Collectors.toSet());

        entities.entrySet().removeIf(entry -> {
            if (present.contains(entry.getKey())) {
                return false;
            }
            despawn(entry.getValue());
            return true;
        });
    }

    /**
     * Retire une entité de façon thread-safe : la suppression est exécutée sous le
     * verrou de l'entité (API Acquirable), car {@link #sync()} peut tourner sur un
     * thread différent de celui qui tick le NPC.
     */
    private static void despawn(ClientEntity entity) {
        entity.getAcquirable().sync(e -> e.remove());
    }

    private ClientEntity spawn(Client client) {
        var memory = new ClientMemory(instance, client, campsite);
        memory.setReceptionPosition(PositionMapper.toMinestomPos(receptionPoint));
        memory.setExitPosition(PositionMapper.toMinestomPos(exitPoint));

        // Les nouveaux arrivants (en attente, sans emplacement) apparaissent à la
        // sortie/entrée et rejoignent l'accueil ; les autres sur leur emplacement.
        var location = client.getPlot() != null ? client.getPlot().getPosition() : exitPoint;
        var spawnPos = PositionMapper.toMinestomPos(location);

        var skin = skinLibrary.randomClientSkin(random).orElse(null);
        var entity = new ClientEntity(memory, skin);
        entity.setInstance(instance, spawnPos);
        memory.setPlayerEntity(entity);
        return entity;
    }

    /** Libère les entités et le listener ; à appeler à la fin de la session. */
    public synchronized void dispose() {
        syncTask.cancel();
        instance.eventNode().removeListener(phaseListener);
        for (var entity : entities.values()) {
            despawn(entity);
        }
        entities.clear();
    }
}
