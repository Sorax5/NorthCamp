package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.client.Client;
import fr.phylisiumstudio.logic.client.ClientArchetype;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fabrique les clients qui se présentent à l'accueil au lever du jour
 * (pattern Factory). Détermine aléatoirement la composition du groupe,
 * la durée de séjour et le budget.
 */
@Singleton
public class ArrivalGenerator {

    private static final int MAX_GROUP_SIZE = 4;
    private static final double FAMILY_CHANCE = 0.35;
    private static final int MIN_STAY_DAYS = 1;
    private static final int MAX_STAY_DAYS = 5;
    private static final double BUDGET_PER_PERSON_PER_DAY = 40.0;
    private static final double BUDGET_VARIANCE = 0.5; // ±50 %

    private final Random random;

    @Inject
    public ArrivalGenerator(Random random) {
        this.random = random;
    }

    /** Crée un client (seul ou groupe) prêt à patienter à l'accueil. */
    public Client generate() {
        int groupSize = random.nextDouble() < FAMILY_CHANCE
                ? 2 + random.nextInt(MAX_GROUP_SIZE - 1)
                : 1;
        int stayDays = MIN_STAY_DAYS + random.nextInt(MAX_STAY_DAYS - MIN_STAY_DAYS + 1);

        double base = BUDGET_PER_PERSON_PER_DAY * groupSize * stayDays;
        double factor = 1.0 + (random.nextDouble() * 2.0 - 1.0) * BUDGET_VARIANCE;
        double budget = Math.round(base * factor);

        var client = new Client(groupSize, stayDays, budget);
        var archetypes = ClientArchetype.values();
        client.setArchetype(archetypes[random.nextInt(archetypes.length)]);
        return client;
    }

    /** Génère un lot d'arrivées. */
    public List<Client> generate(int count) {
        var arrivals = new ArrayList<Client>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            arrivals.add(generate());
        }
        return arrivals;
    }
}
