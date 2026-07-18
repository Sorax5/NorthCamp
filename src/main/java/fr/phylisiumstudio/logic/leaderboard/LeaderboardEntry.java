package fr.phylisiumstudio.logic.leaderboard;

import java.util.UUID;

/**
 * Une ligne de classement : le rang (1 = meilleur), le propriétaire du camping
 * et la valeur atteinte sur la métrique considérée.
 */
public record LeaderboardEntry(int rank, UUID ownerID, UUID campsiteID, double value) {
}
