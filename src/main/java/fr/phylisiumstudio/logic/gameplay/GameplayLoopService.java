package fr.phylisiumstudio.logic.gameplay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.activity.Activity;
import fr.phylisiumstudio.logic.client.ClientLifecycle;
import fr.phylisiumstudio.logic.clock.GamePhase;
import fr.phylisiumstudio.logic.clock.event.PhaseChangeEvent;
import fr.phylisiumstudio.logic.economy.MarketService;
import fr.phylisiumstudio.logic.economy.SatisfactionService;
import fr.phylisiumstudio.logic.economy.SolvencyService;
import fr.phylisiumstudio.logic.plot.PlotUpgradeService;
import fr.phylisiumstudio.logic.rating.RatingService;
import fr.phylisiumstudio.logic.season.SeasonService;
import fr.phylisiumstudio.logic.service.PlotDataService;
import fr.phylisiumstudio.logic.staff.StaffService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestre la boucle de gameplay quotidienne en réagissant aux transitions de
 * phase du cycle jour/nuit (pattern Observer via l'EventHandler global).
 *
 * <p>Au lever du jour : le marché fluctue, les activités s'usent, les clients
 * partis la veille quittent les lieux (impact réputation), les clients trop
 * insatisfaits abandonnent la file, les séjours avancent d'un jour (départs →
 * emplacements sales), puis de nouveaux clients arrivent — en nombre modulé par
 * la réputation et la saison. Enfin, les employés travaillent.
 */
@Singleton
public class GameplayLoopService {
    private static final Logger logger = LoggerFactory.getLogger(GameplayLoopService.class);

    /** Probabilité qu'une activité opérationnelle s'use et tombe en panne chaque jour. */
    private static final double ACTIVITY_DEGRADE_CHANCE = 0.25;
    /** Prime versée par étoile lorsqu'un camping atteint un nouveau palier de note. */
    private static final double STAR_MILESTONE_REWARD_PER_STAR = 500.0;

    private final ClientStayService stayService;
    private final MarketService marketService;
    private final SatisfactionService satisfactionService;
    private final SeasonService seasonService;
    private final StaffService staffService;
    private final PlotDataService plotDataService;
    private final PlotUpgradeService plotUpgradeService;
    private final RatingService ratingService;
    private final EventService eventService;
    private final SolvencyService solvencyService;
    private final Random random;

    /** Solde de chaque camping au matin précédent, pour calculer le bénéfice du jour. */
    private final Map<UUID, Double> lastMorningMoney = new ConcurrentHashMap<>();
    /** Meilleur palier d'étoiles déjà atteint par camping, pour ne récompenser qu'une fois. */
    private final Map<UUID, Integer> bestStars = new ConcurrentHashMap<>();

    @Inject
    public GameplayLoopService(ClientStayService stayService,
                               MarketService marketService,
                               SatisfactionService satisfactionService,
                               SeasonService seasonService,
                               StaffService staffService,
                               PlotDataService plotDataService,
                               PlotUpgradeService plotUpgradeService,
                               RatingService ratingService,
                               EventService eventService,
                               SolvencyService solvencyService,
                               Random random) {
        this.stayService = stayService;
        this.marketService = marketService;
        this.satisfactionService = satisfactionService;
        this.seasonService = seasonService;
        this.staffService = staffService;
        this.plotDataService = plotDataService;
        this.plotUpgradeService = plotUpgradeService;
        this.ratingService = ratingService;
        this.eventService = eventService;
        this.solvencyService = solvencyService;
        this.random = random;

        // Nœud dédié attaché à la racine : regroupe la logique de la boucle et
        // reflète la structure du serveur plutôt que d'empiler sur le handler global.
        var node = EventNode.all("gameplay-loop");
        node.addListener(PhaseChangeEvent.class, this::onPhaseChange);
        MinecraftServer.getGlobalEventHandler().addChild(node);
    }

    private void onPhaseChange(PhaseChangeEvent event) {
        if (event.phase() == GamePhase.DAY) {
            var summary = openNewDay(event.campsite(), event.dayNumber());
            renderSummary(event.getInstance(), summary);
        }
    }

    /**
     * Applique la routine du lever du jour à un camping (départs, abandons,
     * usure, réputation, salaires). Les arrivées, elles, se font en continu
     * pendant la journée via {@link ArrivalService}.
     */
    public DaySummary openNewDay(Campsite campsite, long dayNumber) {
        // Solde de référence : matin précédent (ou solde actuel au tout premier jour).
        double previousMorning = lastMorningMoney.getOrDefault(campsite.getUniqueID(), campsite.getMoney());

        marketService.fluctuate();
        degradeActivities(campsite);

        // Événement du jour (orage, ours, festival) : peut fermer les activités,
        // effrayer les campeurs ou doper la réputation.
        var event = eventService.maybeTrigger(campsite);

        // Les clients en attente perdent patience jour après jour ; trop insatisfaits,
        // ils abandonnent la file (perte + réputation).
        int abandoned = 0;
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() != ClientLifecycle.WAITING) {
                continue;
            }
            satisfactionService.applyWaitingImpatience(client);
            if (satisfactionService.shouldAbandonQueue(client)) {
                satisfactionService.applyQueueAbandonment(campsite);
                client.setLifecycle(ClientLifecycle.GONE);
                abandoned++;
            }
        }

        // Les clients qui partaient hier et n'ont pas fini de sortir quittent
        // définitivement (leur impact réputation a déjà été appliqué à la fin du séjour).
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() == ClientLifecycle.LEAVING) {
                stayService.depart(client);
            }
        }

        // Retire tous les partis (despawn par l'IA, abandons, sorties forcées).
        stayService.removeDeparted(campsite);

        // Revenu passif nocturne des emplacements occupés (croît avec leur niveau).
        collectPlotIncome(campsite);

        // Les séjours avancent ; ceux qui se terminent passent en LEAVING et leur
        // satisfaction finale est reportée maintenant sur la réputation.
        var departing = stayService.advanceDay(campsite);
        for (var client : departing) {
            satisfactionService.applyDeparture(campsite, client);
        }

        // Les employés travaillent : salaires prélevés, puis accueil/nettoyage/maintenance.
        double salaries = staffService.paySalaries(campsite);
        staffService.runAutomation(campsite);

        // Solde négatif : intérêts de dette, érosion de réputation, faillite au-delà du seuil.
        boolean bankrupted = solvencyService.settle(campsite);

        // Note en étoiles : un nouveau palier atteint verse une prime (inclus dans le bénéfice).
        int stars = ratingService.stars(campsite);
        boolean milestone = stars > bestStars.getOrDefault(campsite.getUniqueID(), 0);
        if (milestone) {
            bestStars.put(campsite.getUniqueID(), stars);
            campsite.addMoney(STAR_MILESTONE_REWARD_PER_STAR * stars);
        }

        // Bénéfice du jour = variation de solde depuis le matin précédent.
        double net = campsite.getMoney() - previousMorning;
        lastMorningMoney.put(campsite.getUniqueID(), campsite.getMoney());

        long campers = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.STAYING).count();
        long queue = campsite.getClients().stream()
                .filter(c -> c.getLifecycle() == ClientLifecycle.WAITING).count();

        logger.info("Day {} ({}{}) for campsite {}: {} departures, {} abandoned, {} salaries, net {} (reputation {})",
                dayNumber, seasonService.seasonOf(dayNumber).displayName(),
                seasonService.isSpecialEvent(dayNumber) ? " - événement spécial" : "",
                campsite.getUniqueID(), departing.size(), abandoned,
                salaries, Math.round(net), Math.round(campsite.getReputation()));

        return new DaySummary(dayNumber, seasonService.seasonOf(dayNumber).displayName(),
                seasonService.isSpecialEvent(dayNumber), departing.size(), abandoned,
                salaries, net, campsite.getMoney(), campsite.getReputation(), campers, queue,
                stars, milestone, event, bankrupted);
    }

    /** Affiche le bilan du matin aux joueurs de l'instance du camping. */
    private void renderSummary(Instance instance, DaySummary s) {
        if (instance == null) {
            return;
        }
        var sep = Component.text("──────────────────────────", NamedTextColor.DARK_GRAY);
        var title = Component.text("☀ Bilan — Jour " + s.dayNumber() + " (" + s.season() + ")"
                + (s.specialEvent() ? " ★ événement" : ""), NamedTextColor.GOLD, TextDecoration.BOLD);

        var netColor = s.net() >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        var netText = (s.net() >= 0 ? "+" : "") + Math.round(s.net()) + " $";

        instance.sendMessage(sep);
        instance.sendMessage(title);
        if (s.event() != null) {
            instance.sendMessage(Component.text(s.event().displayName() + " — " + s.event().description(),
                    s.event().positive() ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD));
        }
        instance.sendMessage(line("Bénéfice du jour", netText, netColor));
        instance.sendMessage(line("Salaires versés", "-" + Math.round(s.salaries()) + " $", NamedTextColor.YELLOW));
        instance.sendMessage(line("Départs / abandons", s.departures() + " / " + s.abandoned(),
                s.abandoned() > 0 ? NamedTextColor.RED : NamedTextColor.GRAY));
        instance.sendMessage(line("Campeurs / file", s.campers() + " / " + s.queue(), NamedTextColor.AQUA));
        instance.sendMessage(line("Solde", Math.round(s.money()) + " $", NamedTextColor.GREEN));
        instance.sendMessage(line("Réputation", Math.round(s.reputation()) + " / 100", NamedTextColor.LIGHT_PURPLE));
        instance.sendMessage(line("Note", RatingService.render(s.stars()), NamedTextColor.GOLD));
        if (s.bankrupted()) {
            instance.sendMessage(Component.text("⚠ Faillite ! Renflouement d'urgence : solde remis à zéro, réputation lourdement touchée.",
                    NamedTextColor.RED, TextDecoration.BOLD));
        } else if (s.money() < 0) {
            instance.sendMessage(Component.text("⚠ Solde négatif : intérêts de dette et réputation en baisse. Redressez la barre !",
                    NamedTextColor.RED));
        }
        if (s.starMilestone()) {
            instance.sendMessage(Component.text("★ Nouveau palier ! Prime de "
                    + Math.round(STAR_MILESTONE_REWARD_PER_STAR * s.stars()) + " $ versée.",
                    NamedTextColor.GOLD, TextDecoration.BOLD));
        }
        instance.sendMessage(sep);
    }

    private static Component line(String label, String value, NamedTextColor valueColor) {
        return Component.text()
                .append(Component.text(label + " : ", NamedTextColor.GRAY))
                .append(Component.text(value, valueColor))
                .build();
    }

    /** Ajoute le revenu par nuit de chaque emplacement occupé par un client en séjour. */
    private void collectPlotIncome(Campsite campsite) {
        double income = 0;
        for (var client : campsite.getClients()) {
            if (client.getLifecycle() != ClientLifecycle.STAYING || client.getPlot() == null) {
                continue;
            }
            var plot = client.getPlot();
            income += plotUpgradeService.nightlyIncome(plotDataService.getPlotData(plot.getPlotType()), plot);
        }
        if (income > 0) {
            campsite.addMoney(income);
        }
    }

    /** Usure quotidienne : chaque activité opérationnelle peut tomber en panne. */
    private void degradeActivities(Campsite campsite) {
        for (Activity activity : campsite.getActivities()) {
            if (activity.isOperational() && random.nextDouble() < ACTIVITY_DEGRADE_CHANCE) {
                activity.setOperational(false);
            }
        }
    }

}
