package fr.phylisiumstudio.logic.economy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Marché local virtuel : maintient un prix « juste » par type d'emplacement,
 * qui fluctue chaque jour. Le joueur doit aligner ses tarifs sur ce marché,
 * sinon les clients jugent le prix trop élevé et se démotivent.
 */
@Singleton
public class MarketService {

    /** Prix juste de base par type d'emplacement (par nuit). */
    private static final Map<PlotType, Double> BASE_FAIR_PRICE = new EnumMap<>(PlotType.class);
    static {
        BASE_FAIR_PRICE.put(PlotType.CAMPSITE, 50.0);
        BASE_FAIR_PRICE.put(PlotType.CARAVAN, 80.0);
    }

    private static final double DEFAULT_FAIR_PRICE = 50.0;
    private static final double MAX_DAILY_DRIFT = 0.15; // ±15 % par jour
    private static final double MIN_FACTOR = 0.5;
    private static final double MAX_FACTOR = 1.8;

    private final Random random;
    private final Map<PlotType, Double> fairPrice = new EnumMap<>(PlotType.class);

    @Inject
    public MarketService(Random random) {
        this.random = random;
        for (var type : PlotType.values()) {
            fairPrice.put(type, BASE_FAIR_PRICE.getOrDefault(type, DEFAULT_FAIR_PRICE));
        }
    }

    public double fairPrice(PlotType type) {
        return fairPrice.getOrDefault(type, DEFAULT_FAIR_PRICE);
    }

    /**
     * Ratio prix demandé / prix juste. {@code 1.0} = aligné, {@code > 1} = plus cher
     * que le marché, {@code < 1} = bonne affaire.
     */
    public double priceRatio(Plot plot) {
        double fair = fairPrice(plot.getPlotType());
        if (fair <= 0) {
            return 1.0;
        }
        return plot.getPrice() / fair;
    }

    /** Fait dériver les prix justes d'un cran, à appeler au lever du jour. */
    public void fluctuate() {
        for (var type : PlotType.values()) {
            double base = BASE_FAIR_PRICE.getOrDefault(type, DEFAULT_FAIR_PRICE);
            double drift = (random.nextDouble() * 2.0 - 1.0) * MAX_DAILY_DRIFT;
            double next = fairPrice(type) * (1.0 + drift);
            next = Math.max(base * MIN_FACTOR, Math.min(base * MAX_FACTOR, next));
            fairPrice.put(type, Math.round(next * 100.0) / 100.0);
        }
    }
}
