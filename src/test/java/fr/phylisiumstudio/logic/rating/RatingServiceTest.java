package fr.phylisiumstudio.logic.rating;

import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.client.Client;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RatingServiceTest {

    private final RatingService service = new RatingService();

    @Test
    void starsFollowScoreThresholds() {
        assertEquals(0, RatingService.starsOf(10));
        assertEquals(1, RatingService.starsOf(20));
        assertEquals(2, RatingService.starsOf(40));
        assertEquals(3, RatingService.starsOf(60));
        assertEquals(4, RatingService.starsOf(75));
        assertEquals(5, RatingService.starsOf(90));
    }

    @Test
    void renderProducesFiveGlyphs() {
        assertEquals("★★★☆☆", RatingService.render(3));
        assertEquals("☆☆☆☆☆", RatingService.render(0));
        assertEquals("★★★★★", RatingService.render(5));
        assertEquals("★★★★★", RatingService.render(9)); // clampé
    }

    @Test
    void ratingBlendsReputationAndSatisfactionWithClients() {
        var campsite = new Campsite(UUID.randomUUID());
        // Sans client : basé sur la réputation seule (défaut 50 -> 2 étoiles).
        assertEquals(2, service.stars(campsite));

        var happy = new Client(1, 2, 100);
        happy.setSatisfaction(100);
        campsite.addClient(happy);
        // (50 + 100) / 2 = 75 -> 4 étoiles.
        assertEquals(4, service.stars(campsite));
    }
}
