package fr.phylisiumstudio.logic.staff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Employé NPC recruté par le joueur pour automatiser les tâches physiques.
 *
 * <p>Chaque employé possède une aptitude (0–1) par {@link StaffRole} — ses forces
 * et faiblesses — un rôle actuellement assigné, et un salaire prélevé chaque jour
 * sur les finances du camping.
 */
@Data
public class Staff {
    private final UUID uniqueId;
    private final String name;

    /** Aptitude 0–1 par rôle ; absente = incompétent (0). */
    private final Map<StaffRole, Double> skills;

    /** Salaire quotidien prélevé sur le camping. */
    private final double dailySalary;

    /** Rôle sur lequel l'employé travaille actuellement ({@code null} = inactif). */
    private StaffRole assignedRole;

    @JsonCreator
    public Staff(
            @JsonProperty("uniqueId") UUID uniqueId,
            @JsonProperty("name") String name,
            @JsonProperty("skills") Map<StaffRole, Double> skills,
            @JsonProperty("dailySalary") double dailySalary,
            @JsonProperty("assignedRole") StaffRole assignedRole
    ) {
        this.uniqueId = uniqueId != null ? uniqueId : UUID.randomUUID();
        this.name = name;
        this.skills = skills != null ? new EnumMap<>(skills) : new EnumMap<>(StaffRole.class);
        this.dailySalary = dailySalary;
        this.assignedRole = assignedRole;
    }

    /** Aptitude de l'employé pour un rôle (0 s'il ne le maîtrise pas). */
    public double skill(StaffRole role) {
        return skills.getOrDefault(role, 0.0);
    }

    /** Aptitude sur le rôle actuellement assigné, ou 0 si inactif. */
    public double activeSkill() {
        return assignedRole == null ? 0.0 : skill(assignedRole);
    }
}
