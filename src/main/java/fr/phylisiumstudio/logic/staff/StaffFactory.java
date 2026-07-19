package fr.phylisiumstudio.logic.staff;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Génère des candidats employés (pattern Factory). Chaque candidat a une
 * spécialité (aptitude élevée sur un rôle) et des aptitudes moindres ailleurs,
 * avec un salaire cohérent avec sa compétence dominante.
 */
@Singleton
public class StaffFactory {

    private static final List<String> NAMES = List.of(
            "Alex", "Sam", "Jordan", "Casey", "Riley", "Morgan", "Taylor", "Jamie",
            "Quinn", "Avery", "Charlie", "Robin", "Dakota", "Skyler", "Reese");

    private static final double BASE_SALARY = 60.0;
    private static final double SALARY_PER_SKILL = 120.0;

    private final Random random;

    @Inject
    public StaffFactory(Random random) {
        this.random = random;
    }

    /** Crée un candidat spécialisé dans un rôle tiré au hasard. */
    public Staff generateCandidate() {
        var specialty = StaffRole.values()[random.nextInt(StaffRole.values().length)];

        Map<StaffRole, Double> skills = new EnumMap<>(StaffRole.class);
        for (var role : StaffRole.values()) {
            double skill = role == specialty
                    ? 0.6 + random.nextDouble() * 0.4   // spécialité : 0.6–1.0
                    : random.nextDouble() * 0.4;         // reste : 0.0–0.4
            skills.put(role, Math.round(skill * 100.0) / 100.0);
        }

        double salary = Math.round(BASE_SALARY + skills.get(specialty) * SALARY_PER_SKILL);
        var name = NAMES.get(random.nextInt(NAMES.size()));
        var look = random.nextBoolean() ? StaffLook.VARIANT_A : StaffLook.VARIANT_B;

        return new Staff(UUID.randomUUID(), name, skills, salary, look, specialty, null);
    }

    public List<Staff> generateCandidates(int count) {
        return Stream.generate(this::generateCandidate)
                .limit(Math.max(0, count))
                .toList();
    }
}
