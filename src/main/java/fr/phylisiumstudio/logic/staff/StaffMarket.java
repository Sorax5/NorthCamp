package fr.phylisiumstudio.logic.staff;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vivier de candidats à l'embauche, par camping. Les candidats sont générés à la
 * demande et gardés en mémoire le temps que le joueur en recrute via le menu.
 */
@Singleton
public class StaffMarket {

    private static final int CANDIDATE_POOL_SIZE = 3;

    private final StaffFactory staffFactory;
    private final Map<UUID, List<Staff>> candidatesByCampsite = new ConcurrentHashMap<>();

    @Inject
    public StaffMarket(StaffFactory staffFactory) {
        this.staffFactory = staffFactory;
    }

    /** Candidats actuellement proposés à un camping, générés au premier accès. */
    public List<Staff> candidates(UUID campsiteId) {
        return candidatesByCampsite.computeIfAbsent(campsiteId,
                _ -> staffFactory.generateCandidates(CANDIDATE_POOL_SIZE));
    }

    /** Régénère le vivier (nouvelle fournée de candidats). */
    public void refresh(UUID campsiteId) {
        candidatesByCampsite.put(campsiteId, staffFactory.generateCandidates(CANDIDATE_POOL_SIZE));
    }

    /** Retire un candidat du vivier (une fois recruté). */
    public void remove(UUID campsiteId, UUID candidateId) {
        var list = candidatesByCampsite.get(campsiteId);
        if (list != null) {
            list.removeIf(s -> s.getUniqueId().equals(candidateId));
        }
    }
}
