package fr.phylisiumstudio.logic.effect;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;

/**
 * Petits effets visuels (particules) diffusés aux joueurs d'une instance pour
 * souligner les événements : affectation d'un client, fin d'activité, départ…
 */
public final class Effects {
    private Effects() {
    }

    private static final Vec SPREAD = new Vec(0.35, 0.5, 0.35);

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
}
