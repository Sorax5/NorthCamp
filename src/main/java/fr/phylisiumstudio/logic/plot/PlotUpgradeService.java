package fr.phylisiumstudio.logic.plot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;

/**
 * Améliore les emplacements de camping. Monter un emplacement de niveau coûte le
 * prix défini dans ses {@link PlotData#levels()} et augmente son revenu par nuit
 * (revenu passif tant qu'un client y séjourne). C'est le principal levier
 * d'escalade : réinvestir ses gains pour faire grossir le rendement.
 */
@Singleton
public class PlotUpgradeService {

    private final EconomyService economyService;

    @Inject
    public PlotUpgradeService(EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Existe-t-il un niveau supérieur défini pour cet emplacement ? */
    public boolean canUpgrade(PlotData data, Plot plot) {
        return data != null && plot.getLevel() + 1 < data.levels().size();
    }

    /** Coût pour passer au niveau suivant, ou {@code -1} si déjà au maximum. */
    public long nextCost(PlotData data, Plot plot) {
        return canUpgrade(data, plot) ? data.levels().get(plot.getLevel() + 1).price() : -1;
    }

    /** Revenu par nuit de l'emplacement à son niveau courant. */
    public long nightlyIncome(PlotData data, Plot plot) {
        if (data == null || data.levels().isEmpty()) {
            return 0;
        }
        int index = Math.min(plot.getLevel(), data.levels().size() - 1);
        return data.levels().get(index).income();
    }

    /**
     * Monte l'emplacement d'un niveau si un niveau supérieur existe et que le
     * camping a les fonds.
     *
     * @return {@code true} si l'amélioration a été appliquée.
     */
    public boolean upgrade(Campsite campsite, PlotData data, Plot plot) {
        if (!canUpgrade(data, plot)) {
            return false;
        }
        long cost = data.levels().get(plot.getLevel() + 1).price();
        if (campsite.getMoney() < cost) {
            return false;
        }
        economyService.charge(campsite, cost);
        plot.setLevel(plot.getLevel() + 1);
        return true;
    }
}
