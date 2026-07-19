package fr.phylisiumstudio.logic.gameplay;

/**
 * Bilan d'une journée écoulée, présenté au joueur au lever du jour suivant.
 *
 * <p>{@code net} est la variation d'argent depuis le matin précédent (revenus des
 * loyers et activités de la journée, moins salaires et dépenses) : le « bénéfice
 * du jour » que le joueur veut voir grimper.
 */
public record DaySummary(
        long dayNumber,
        String season,
        boolean specialEvent,
        int departures,
        int abandoned,
        double salaries,
        double net,
        double money,
        double reputation,
        long campers,
        long queue,
        int stars,
        boolean starMilestone
) {
}
