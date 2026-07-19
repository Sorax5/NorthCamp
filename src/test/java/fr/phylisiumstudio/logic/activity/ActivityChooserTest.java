package fr.phylisiumstudio.logic.activity;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ActivityChooserTest {

    private static Activity activity(ActivityType type) {
        return new Activity(new Vector3d(0, 69, 0), 10, 5, 4, type);
    }

    @Test
    void dayAffinityWeightsHigherDuringDay() {
        assertEquals(3, ActivityChooser.weight(activity(ActivityType.SWIM), true));   // DAY, jour
        assertEquals(1, ActivityChooser.weight(activity(ActivityType.SWIM), false));  // DAY, nuit
        assertEquals(3, ActivityChooser.weight(activity(ActivityType.BARBECUE), false)); // NIGHT, nuit
        assertEquals(1, ActivityChooser.weight(activity(ActivityType.BARBECUE), true));  // NIGHT, jour
    }

    @Test
    void chooseFavorsMatchingAffinityOverMany() {
        var activities = List.of(activity(ActivityType.SWIM), activity(ActivityType.BARBECUE));
        int barbecueAtNight = 0;
        var random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            if (ActivityChooser.choose(activities, false, random).orElseThrow().getType() == ActivityType.BARBECUE) {
                barbecueAtNight++;
            }
        }
        // Barbecue (NIGHT) doit dominer la nuit (poids 3 contre 1).
        assertTrue(barbecueAtNight > 600, "barbecue should dominate at night, got " + barbecueAtNight);
    }

    @Test
    void emptyListReturnsEmpty() {
        assertTrue(ActivityChooser.choose(List.of(), true, new Random()).isEmpty());
    }
}
