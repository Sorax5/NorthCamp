package fr.phylisiumstudio.logic.season;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeasonServiceTest {

    private final SeasonService service = new SeasonService();

    @Test
    void firstDayIsSpring() {
        assertEquals(Season.SPRING, service.seasonOf(1));
        assertEquals(Season.SPRING, service.seasonOf(SeasonService.DAYS_PER_SEASON));
    }

    @Test
    void seasonsCycleEverySeasonLength() {
        assertEquals(Season.SUMMER, service.seasonOf(SeasonService.DAYS_PER_SEASON + 1));
        assertEquals(Season.WINTER, service.seasonOf(SeasonService.DAYS_PER_SEASON * 3 + 1));
        // Après 4 saisons, retour au printemps
        assertEquals(Season.SPRING, service.seasonOf(SeasonService.DAYS_PER_SEASON * 4 + 1));
    }

    @Test
    void specialEventBoostsMultiplier() {
        assertTrue(service.isSpecialEvent(10));
        assertFalse(service.isSpecialEvent(11));
        // Le jour 10 est en été (jour 8-14) : multiplicateur saison * événement
        assertEquals(Season.SUMMER.arrivalMultiplier() * 1.5, service.arrivalMultiplier(10), 1e-9);
    }
}
