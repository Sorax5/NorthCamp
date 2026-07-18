package fr.phylisiumstudio.logic.service;

import fr.phylisiumstudio.logic.Campsite;
import org.joml.Vector3d;

import java.util.stream.Stream;

/**
 * Plage rectangulaire de chunks (inclusive) à charger pour une instance.
 *
 * <p>Calculée à partir des positions des emplacements et activités d'un camping,
 * avec une marge pour couvrir les schématics débordant sur les chunks voisins.
 */
public record ChunkRange(int fromX, int toX, int fromZ, int toZ) {

    public int count() {
        return (toX - fromX + 1) * (toZ - fromZ + 1);
    }

    private static int chunkOf(double worldCoord) {
        return (int) Math.floor(worldCoord / 16.0);
    }

    /**
     * Plage carrée centrée sur l'origine, utilisée quand le camping est vide.
     */
    public static ChunkRange square(int radius) {
        int half = radius / 2;
        return new ChunkRange(-half, half, -half, half);
    }

    /**
     * Englobe toutes les positions de plots/activités du camping, élargie d'une marge.
     * Retourne une plage carrée par défaut si le camping ne contient aucun élément.
     */
    public static ChunkRange forCampsite(Campsite campsite, int margin, int fallbackRadius) {
        if (campsite == null) {
            return square(fallbackRadius);
        }

        var positions = Stream.concat(
                campsite.getPlots().stream().map(p -> p.getPosition()),
                campsite.getActivities().stream().map(a -> a.getPosition()));

        Integer minX = null, maxX = null, minZ = null, maxZ = null;
        for (var it = positions.iterator(); it.hasNext(); ) {
            Vector3d pos = it.next();
            int cx = chunkOf(pos.x());
            int cz = chunkOf(pos.z());
            if (minX == null || cx < minX) minX = cx;
            if (maxX == null || cx > maxX) maxX = cx;
            if (minZ == null || cz < minZ) minZ = cz;
            if (maxZ == null || cz > maxZ) maxZ = cz;
        }

        if (minX == null) {
            return square(fallbackRadius);
        }
        return new ChunkRange(minX - margin, maxX + margin, minZ - margin, maxZ + margin);
    }
}
