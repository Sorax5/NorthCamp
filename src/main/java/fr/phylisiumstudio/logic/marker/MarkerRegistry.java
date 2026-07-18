package fr.phylisiumstudio.logic.marker;

import com.google.inject.Singleton;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conserve les marqueurs résolus de chaque emplacement/activité posé, indexés par
 * leur identifiant. Renseigné par les builders à la pose du schématic, il est
 * consulté par l'IA des NPC (qui n'est pas injectée) via {@link #instance()}.
 */
@Singleton
public class MarkerRegistry {

    private static volatile MarkerRegistry instance;

    private final ConcurrentHashMap<UUID, MarkerSet> byId = new ConcurrentHashMap<>();

    public MarkerRegistry() {
        instance = this;
    }

    /** Accès global pour les tâches d'IA non gérées par l'injection de dépendances. */
    public static MarkerRegistry instance() {
        var current = instance;
        return current != null ? current : new MarkerRegistry();
    }

    public void register(UUID id, MarkerSet markers) {
        byId.put(id, markers);
    }

    public void remove(UUID id) {
        byId.remove(id);
    }

    /** Marqueurs d'un élément, ou un ensemble vide (tout se rabat sur les défauts). */
    public MarkerSet get(UUID id) {
        return byId.getOrDefault(id, MarkerSet.empty());
    }
}
