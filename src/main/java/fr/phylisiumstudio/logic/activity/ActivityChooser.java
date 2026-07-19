package fr.phylisiumstudio.logic.activity;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Choisit une activité parmi celles disponibles en pondérant selon l'heure : une
 * activité dont l'affinité correspond au moment de la journée est plus probable,
 * sans exclure les autres (les clients ne suivent pas tous la même routine).
 */
public final class ActivityChooser {
    private ActivityChooser() {
    }

    private static final int MATCH_WEIGHT = 3;
    private static final int ANY_WEIGHT = 2;
    private static final int MISMATCH_WEIGHT = 1;

    /** Poids d'une activité selon la phase courante. */
    public static int weight(Activity activity, boolean isDay) {
        var affinity = activity.getType().affinity();
        if (affinity == TimeAffinity.ANY) {
            return ANY_WEIGHT;
        }
        boolean matches = (affinity == TimeAffinity.DAY) == isDay;
        return matches ? MATCH_WEIGHT : MISMATCH_WEIGHT;
    }

    /** Tire une activité pondérée par l'heure, ou vide si la liste est vide. */
    public static Optional<Activity> choose(List<Activity> activities, boolean isDay, Random random) {
        if (activities.isEmpty()) {
            return Optional.empty();
        }
        int total = activities.stream().mapToInt(a -> weight(a, isDay)).sum();
        int roll = random.nextInt(total);
        int cumulative = 0;
        for (var activity : activities) {
            cumulative += weight(activity, isDay);
            if (roll < cumulative) {
                return Optional.of(activity);
            }
        }
        return Optional.of(activities.get(activities.size() - 1));
    }
}
