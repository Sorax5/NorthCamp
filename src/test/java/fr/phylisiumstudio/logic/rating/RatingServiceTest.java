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
    void priceMultiplierGrowsWithStars() {
        var campsite = new Campsite(UUID.randomUUID());
        // Réputation par défaut 50 -> 2 étoiles -> 1 + 2*0.08 = 1.16.
        assertEquals(1.0 + 2 * RatingService.STAR_PRICE_BONUS, RatingService.priceMultiplier(campsite), 1e-9);

        // Réputation au plafond -> 5 étoiles -> 1.40.
        campsite.adjustReputation(100);
        assertEquals(1.0 + 5 * RatingService.STAR_PRICE_BONUS, RatingService.priceMultiplier(campsite), 1e-9);
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
