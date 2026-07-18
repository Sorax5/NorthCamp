package fr.phylisiumstudio.logic.clock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameClockTest {

    @Test
    void startsOnDayOne() {
        var clock = new GameClock(3, 2);
        assertEquals(GamePhase.DAY, clock.getPhase());
        assertEquals(1, clock.getDayNumber());
    }

    @Test
    void transitionsToNightAfterDayDuration() {
        var clock = new GameClock(3, 2);
        assertFalse(clock.tick()); // 1
        assertFalse(clock.tick()); // 2
        assertTrue(clock.tick());  // 3 -> transition
        assertEquals(GamePhase.NIGHT, clock.getPhase());
        assertEquals(1, clock.getDayNumber()); // le jour n'avance pas en entrant en nuit
    }

    @Test
    void newDayIncrementsCounterOnDaybreak() {
        var clock = new GameClock(2, 2);
        clock.tick(); clock.tick(); // -> NIGHT
        clock.tick(); clock.tick(); // -> DAY (jour 2)
        assertEquals(GamePhase.DAY, clock.getPhase());
        assertEquals(2, clock.getDayNumber());
    }

    @Test
    void rejectsNonPositiveDurations() {
        assertThrows(IllegalArgumentException.class, () -> new GameClock(0, 5));
    }

    @Test
    void minecraftTimeProgressesAcrossDayAndNight() {
        var clock = new GameClock(4, 4);
        assertEquals(0, clock.minecraftTime());        // début du jour
        clock.tick();
        assertEquals(3000, clock.minecraftTime());     // 1/4 du jour -> 3000
        clock.tick();
        assertEquals(6000, clock.minecraftTime());     // midi
        clock.tick(); clock.tick();                    // -> transition vers la nuit
        assertEquals(GamePhase.NIGHT, clock.getPhase());
        assertEquals(12000, clock.minecraftTime());    // début de nuit
        clock.tick(); clock.tick();
        assertEquals(18000, clock.minecraftTime());    // minuit
    }
}
