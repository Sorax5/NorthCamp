package fr.phylisiumstudio.logic.staff;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityCreature;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.player.ResolvableProfile;
import org.joml.Vector3d;

import java.time.Duration;
import java.time.Instant;

/**
 * NPC employé. Comme un client, il cherche la tâche de son rôle, s'y rend, puis
 * l'accomplit sur place dès que possible. La cadence dépend de sa compétence :
 * un employé doué enchaîne les tâches plus vite.
 */
public class StaffEntity extends EntityCreature {

    private static final int RETARGET_INTERVAL = 20; // ~1 s
    private static final double ARRIVAL_DISTANCE = 2.0;
    private static final Duration MAX_WORK_COOLDOWN = Duration.ofSeconds(4);
    private static final Duration MIN_WORK_COOLDOWN = Duration.ofMillis(800);
    /** En dessous de ce déplacement entre deux pas, l'employé est considéré immobile. */
    private static final double STUCK_EPSILON = 0.05;
    /** Pas consécutifs sans progrès (≈ secondes) avant déblocage automatique. */
    private static final int STUCK_LIMIT = 5;

    private final Staff staff;
    private final Campsite campsite;
    private final StaffBrain brain;
    private final Vector3d reception;
    private final Duration workCooldown;

    private long tickCounter;
    private Instant lastWork = Instant.EPOCH;
    private Pos lastPosition;
    private int stuckSteps;

    public java.util.UUID staffId() {
        return staff.getUniqueId();
    }

    public StaffEntity(Staff staff, PlayerSkin skin, Campsite campsite, StaffBrain brain, Vector3d reception) {
        super(EntityType.MANNEQUIN);
        this.staff = staff;
        this.campsite = campsite;
        this.brain = brain;
        this.reception = reception;
        this.workCooldown = cooldownFor(staff);

        editEntityMeta(MannequinMeta.class, meta -> {
            meta.setNotifyAboutChanges(false);
            if (skin != null) {
                meta.setProfile(new ResolvableProfile(skin));
            }
            meta.setCustomNameVisible(true);
            meta.setNotifyAboutChanges(true);
        });

        getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.25);
        updateName("prêt");
    }

    /** Cadence de travail : plus la compétence du rôle est haute, plus c'est court. */
    private static Duration cooldownFor(Staff staff) {
        double skill = staff.getAssignedRole() == null ? 0 : staff.skill(staff.getAssignedRole());
        long range = MAX_WORK_COOLDOWN.minus(MIN_WORK_COOLDOWN).toMillis();
        long millis = MAX_WORK_COOLDOWN.toMillis() - Math.round(range * Math.max(0, Math.min(1, skill)));
        return Duration.ofMillis(millis);
    }

    @Override
    public void tick(long time) {
        super.tick(time);
        if (tickCounter++ % RETARGET_INTERVAL != 0) {
            return;
        }
        try {
            step();
        } catch (Exception ignored) {
            // Une erreur de tâche ne doit pas figer l'employé.
        }
    }

    private void step() {
        var targetVec = brain.target(staff, campsite, reception);
        var target = PositionMapper.toMinestomPos(targetVec);

        if (getPosition().distance(target) > ARRIVAL_DISTANCE) {
            // Détection d'un employé coincé (pathfinding bloqué) : aucun progrès
            // pendant plusieurs pas → déblocage automatique par téléportation.
            if (lastPosition != null && getPosition().distance(lastPosition) < STUCK_EPSILON) {
                if (++stuckSteps >= STUCK_LIMIT) {
                    unstick();
                    return;
                }
            } else {
                stuckSteps = 0;
            }
            lastPosition = getPosition();

            updateName("va travailler");
            getNavigator().setPathTo(target);
            return;
        }

        stuckSteps = 0;
        // Sur place : accomplir une tâche si la cadence le permet.
        if (Duration.between(lastWork, Instant.now()).compareTo(workCooldown) >= 0) {
            boolean did = brain.work(staff, campsite);
            lastWork = Instant.now();
            updateName(did ? "au travail" : "en attente");
        }
    }

    /**
     * Débloque l'employé : le ramène à l'accueil, purge son chemin coincé et
     * réarme son travail. Appelé automatiquement (immobilité) ou manuellement
     * ({@code /staff wake}).
     */
    public void unstick() {
        stuckSteps = 0;
        lastPosition = null;
        lastWork = Instant.EPOCH;
        getNavigator().setPathTo(null);
        teleport(PositionMapper.toMinestomPos(reception));
        updateName("débloqué");
    }

    private void updateName(String status) {
        var role = staff.getAssignedRole();
        var label = Component.text()
                .append(Component.text(staff.getName(), NamedTextColor.GOLD))
                .append(Component.text(" — " + (role != null ? role.name() : "inactif")
                        + " (" + status + ")", NamedTextColor.GRAY))
                .build();
        this.set(DataComponents.CUSTOM_NAME, label);
    }
}
