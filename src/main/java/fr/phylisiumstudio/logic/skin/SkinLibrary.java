package fr.phylisiumstudio.logic.skin;

import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.staff.StaffLook;
import net.minestom.server.entity.PlayerSkin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Bibliothèque de skins pré-résolus. Les clients reçoivent un skin aléatoire ;
 * les employés partagent deux skins fixes (homme / femme) selon leur genre.
 *
 * <p>La résolution passe par Mojang (appel HTTP bloquant) : elle est faite une
 * seule fois au démarrage, en parallèle et hors du thread de tick, pour ne pas
 * impacter les TPS.
 */
@Singleton
public class SkinLibrary {
    private static final Logger logger = LoggerFactory.getLogger(SkinLibrary.class);

    /** Comptes aux skins variés utilisés pour les clients. */
    private static final List<String> CLIENT_USERNAMES = List.of(
            "Notch", "jeb_", "Dinnerbone", "SoraxDubbing", "Hisawax", "Pugile", "Technoblade", "LukiEnLive");

    /** Deux skins d'employé cosmétiques, sans signification identitaire. */
    private static final String STAFF_USERNAME_A = "Dragonbyte404";
    private static final String STAFF_USERNAME_B = "TargetEngineer";

    private final List<PlayerSkin> clientSkins = new ArrayList<>();
    private PlayerSkin staffSkinA;
    private PlayerSkin staffSkinB;

    /** Résout tous les skins en parallèle. À appeler une fois au démarrage. */
    public void load() {
        try {
            var clientFutures = CLIENT_USERNAMES.stream()
                    .map(name -> CompletableFuture.supplyAsync(() -> PlayerSkin.fromUsername(name)))
                    .toList();
            var staffFutureA = CompletableFuture.supplyAsync(() -> PlayerSkin.fromUsername(STAFF_USERNAME_A));
            var staffFutureB = CompletableFuture.supplyAsync(() -> PlayerSkin.fromUsername(STAFF_USERNAME_B));

            for (var future : clientFutures) {
                var skin = future.join();
                if (skin != null) {
                    clientSkins.add(skin);
                }
            }
            staffSkinA = staffFutureA.join();
            staffSkinB = staffFutureB.join();

            logger.info("Loaded {} client skins (+ 2 staff skins)", clientSkins.size());
        } catch (Exception e) {
            logger.warn("Failed to load some skins; NPCs will use default appearance", e);
        }
    }

    /** Skin client aléatoire, ou vide si aucun n'a pu être chargé. */
    public Optional<PlayerSkin> randomClientSkin(Random random) {
        if (clientSkins.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(clientSkins.get(random.nextInt(clientSkins.size())));
    }

    /** Skin d'employé selon la variante cosmétique choisie. */
    public Optional<PlayerSkin> staffSkin(StaffLook look) {
        return Optional.ofNullable(look == StaffLook.VARIANT_B ? staffSkinB : staffSkinA);
    }
}
