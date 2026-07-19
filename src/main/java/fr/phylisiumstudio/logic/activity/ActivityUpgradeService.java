package fr.phylisiumstudio.logic.activity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.economy.EconomyService;

/**
 * Améliore les activités. Monter une activité de niveau coûte le prix défini dans
 * ses {@link ActivityData#levels()} et augmente sa capacité (plus de clients
 * simultanés) ainsi que son revenu par passage. Pendant du levier d'upgrade des
 * emplacements, appliqué aux activités.
 */
@Singleton
public class ActivityUpgradeService {

    private final EconomyService economyService;

    @Inject
    public ActivityUpgradeService(EconomyService economyService) {
        this.economyService = economyService;
    }

    /** Existe-t-il un niveau supérieur défini pour cette activité ? */
    public boolean canUpgrade(ActivityData data, Activity activity) {
        return data != null && activity.getCurrentLevel() + 1 < data.levels().size();
    }

    /** Coût pour passer au niveau suivant, ou {@code -1} si déjà au maximum. */
    public long nextCost(ActivityData data, Activity activity) {
        return canUpgrade(data, activity) ? data.levels().get(activity.getCurrentLevel() + 1).price() : -1;
    }

    /**
     * Monte l'activité d'un niveau : +1 client simultané et revenu par passage
     * augmenté du gain de palier.
     *
     * @return {@code true} si l'amélioration a été appliquée.
     */
    public boolean upgrade(Campsite campsite, ActivityData data, Activity activity) {
        if (!canUpgrade(data, activity)) {
            return false;
        }
        int nextLevel = activity.getCurrentLevel() + 1;
        long cost = data.levels().get(nextLevel).price();
        if (campsite.getMoney() < cost) {
            return false;
        }
        economyService.charge(campsite, cost);
        activity.setCurrentLevel(nextLevel);
        activity.setMaxClients(activity.getMaxClients() + 1);
        activity.setPrice(activity.getPrice() + data.levels().get(nextLevel).income());
        return true;
    }
}
