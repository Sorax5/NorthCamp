package fr.phylisiumstudio.logic.effect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.TaskSchedule;

/**
 * Petits effets visuels (particules) diffusés aux joueurs d'une instance pour
 * souligner les événements : affectation d'un client, fin d'activité, départ…
 */
public final class Effects {
    private Effects() {
    }

    private static final Vec SPREAD = new Vec(0.35, 0.5, 0.35);

    private static final EntityType TEXT_DISPLAY = EntityType.fromKey("minecraft:text_display");
    private static final int POPUP_TICKS = 24;
    private static final double POPUP_RISE_PER_TICK = 0.04;

    private static final Particle HEART = Particle.fromKey("minecraft:heart");
    private static final Particle HAPPY_VILLAGER = Particle.fromKey("minecraft:happy_villager");
    private static final Particle POOF = Particle.fromKey("minecraft:poof");

    /** Diffuse un petit nuage de particules autour d'une position. */
    public static void burst(Instance instance, Point position, Particle particle, int count) {
        if (instance == null || position == null || particle == null) {
            return;
        }
        var packet = new ParticlePacket(particle, position, SPREAD, 0.02f, count);
        instance.sendGroupedPacket(packet);
    }

    /** Client installé sur un emplacement (cœurs). */
    public static void assigned(Instance instance, Point position) {
        burst(instance, position, HEART, 8);
    }

    /** Activité terminée (villageois content). */
    public static void activityDone(Instance instance, Point position) {
        burst(instance, position, HAPPY_VILLAGER, 10);
    }

    /** Départ / despawn (bouffée de fumée). */
    public static void leaving(Instance instance, Point position) {
        burst(instance, position, POOF, 12);
    }

    /**
     * Fait apparaître un texte flottant « +X $ » (ou « -X $ ») qui monte puis
     * s'efface, pour souligner un gain (ou une perte) à l'endroit où il se produit.
     * L'animation tourne sur le thread de tick de l'instance (manipulation d'entité sûre).
     */
    public static void moneyPopup(Instance instance, Point position, double amount) {
        long rounded = Math.round(amount);
        if (instance == null || position == null || rounded == 0) {
            return;
        }
        boolean gain = rounded > 0;
        var color = gain ? NamedTextColor.GREEN : NamedTextColor.RED;
        var label = (gain ? "+" : "") + rounded + " $";

        var text = new Entity(TEXT_DISPLAY);
        text.setNoGravity(true);
        text.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
            meta.setText(Component.text(label, color));
        });
        text.setInstance(instance, position.add(0, 1.2, 0));

        int[] ticks = {0};
        instance.scheduler().submitTask(() -> {
            ticks[0]++;
            text.teleport(text.getPosition().add(0, POPUP_RISE_PER_TICK, 0));
            if (ticks[0] >= POPUP_TICKS) {
                text.remove();
                return TaskSchedule.stop();
            }
            return TaskSchedule.tick(1);
        });
    }
}
