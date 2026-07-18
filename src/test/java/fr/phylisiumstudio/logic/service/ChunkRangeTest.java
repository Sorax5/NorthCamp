package fr.phylisiumstudio.logic.service;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChunkRangeTest {

    @Test
    void emptyCampsiteFallsBackToSquare() {
        var campsite = new Campsite(UUID.randomUUID());
        var range = ChunkRange.forCampsite(campsite, 2, 12);
        // radius 12 -> half 6, carré [-6..6]
        assertEquals(-6, range.fromX());
        assertEquals(6, range.toX());
        assertEquals(13 * 13, range.count());
    }

    @Test
    void boundsCoverPlotsWithMargin() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addPlot(new Plot(new Vector3d(0, 69, 0), PlotType.CAMPSITE));   // chunk 0,0
        campsite.addPlot(new Plot(new Vector3d(40, 69, 32), PlotType.CAMPSITE)); // chunk 2,2

        var range = ChunkRange.forCampsite(campsite, 2, 12);
        assertEquals(-2, range.fromX()); // 0 - margin
        assertEquals(4, range.toX());    // 2 + margin
        assertEquals(-2, range.fromZ());
        assertEquals(4, range.toZ());
    }

    @Test
    void negativeCoordinatesFloorCorrectly() {
        var campsite = new Campsite(UUID.randomUUID());
        campsite.addPlot(new Plot(new Vector3d(-1, 69, -1), PlotType.CAMPSITE)); // chunk -1,-1
        var range = ChunkRange.forCampsite(campsite, 0, 12);
        assertEquals(-1, range.fromX());
        assertEquals(-1, range.toX());
    }
}
