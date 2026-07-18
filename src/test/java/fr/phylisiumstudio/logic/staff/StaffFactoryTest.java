package fr.phylisiumstudio.logic.staff;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StaffFactoryTest {

    @Test
    void candidateHasSpecialtyStrongerThanFallbackSkills() {
        var factory = new StaffFactory(new Random(7));
        for (int i = 0; i < 50; i++) {
            var staff = factory.generateCandidate();
            var specialty = staff.getAssignedRole();
            assertNotNull(specialty);
            assertTrue(staff.skill(specialty) >= 0.6, "specialty skill should be high");
            assertTrue(staff.getDailySalary() > 0);
            for (var role : StaffRole.values()) {
                assertTrue(staff.skill(role) >= 0.0 && staff.skill(role) <= 1.0);
            }
        }
    }

    @Test
    void generatesRequestedCount() {
        var factory = new StaffFactory(new Random(1));
        assertEquals(3, factory.generateCandidates(3).size());
    }
}
