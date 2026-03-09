package fr.phylisiumstudio.logic.builder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import fr.phylisiumstudio.logic.area.AreaBlockIterator;
import fr.phylisiumstudio.logic.mapper.PositionMapper;
import fr.phylisiumstudio.logic.mapper.VectorMapper;
import fr.phylisiumstudio.logic.plot.Plot;
import fr.phylisiumstudio.logic.plot.PlotData;
import fr.phylisiumstudio.logic.schematic.SchematicFactory;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.util.Rotation;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class PlotBuilder extends MinestomBuilder<PlotData, Plot> {
    private final SchematicFactory schematicFactory;
    private final Logger logger = LoggerFactory.getLogger(PlotBuilder.class);

    @Inject
    public PlotBuilder(SchematicFactory schematicFactory) {
        this.schematicFactory = schematicFactory;
    }

    // Méthode utilitaire publique pour récupérer la liste des entités armor_stand
    public static List<Object> collectArmorStandEntities(Schematic schematic) {
        List<Object> armorEntities = new ArrayList<>();
        if (schematic == null) return armorEntities;

        for (var entity : schematic.entities()) {
            try {
                boolean isArmor = false;
                if (entity instanceof CompoundBinaryTag) {
                    var comp = (CompoundBinaryTag) entity;
                    try {
                        var id = comp.getString("id");
                        if (id != null && id.toLowerCase().contains("armor_stand")) {
                            isArmor = true;
                        }
                    } catch (Exception ignored) {
                    }

                    if (!isArmor && comp.get("Id") != null) {
                        var idObj = comp.get("Id");
                        if (idObj != null && idObj.toString().toLowerCase().contains("armor_stand")) {
                            isArmor = true;
                        }
                    }
                } else {
                    var repr = entity != null ? entity.toString().toLowerCase() : "";
                    if (repr.contains("minecraft:armor_stand") || repr.contains("armor_stand")) {
                        isArmor = true;
                    }
                }

                if (isArmor) armorEntities.add(entity);
            } catch (Exception e) {
                // Ignorer et continuer
            }
        }

        return armorEntities;
    }

    @Override
    public CompletableFuture<Void> BuildAsync(PlotData data, Plot state, InstanceContainer instance) {
        var future = new CompletableFuture<Void>();
        try {
            var schematic = schematicFactory.getSchematic(data.schem());
            var batch = schematic.createBatch(Rotation.NONE);

            // Utilisation de la méthode utilitaire pour récupérer uniquement les armor stands
            List<Object> schematicEntities = collectArmorStandEntities(schematic);

            batch.apply(instance, PositionMapper.toMinestomPos(state.getPosition()), blockBatch -> {
                var area = data.area();

                var areaBlockIterator = new AreaBlockIterator(area);
                while (areaBlockIterator.hasNext()) {
                    var vector3i = areaBlockIterator.next();
                    if (!area.isGroundBlock(vector3i) || !area.isWallBlock(vector3i)) {
                        continue;
                    }

                    var wordPosition = VectorMapper.toVector3d(vector3i).add(state.getPosition());
                    var blockPos = PositionMapper.toMinestomPos(wordPosition);
                    blockBatch.setBlock(blockPos, Block.WHITE_WOOL);
                }

                // Pattern pour tenter d'extraire Pos: [ x, y, z ] depuis la représentation en chaîne
                Pattern posPattern = Pattern.compile("Pos:\\s*\\[\\s*([-\\d.]+)\\s*,\\s*([-\\d.]+)\\s*,\\s*([-\\d.]+)");

                for (var entity : schematicEntities) {
                    try {
                        String id = null;
                        double ex = Double.NaN, ey = Double.NaN, ez = Double.NaN;

                        if (entity instanceof CompoundBinaryTag) {
                            try {
                                var comp = (CompoundBinaryTag) entity;
                                // Essayer d'obtenir l'id depuis le tag NBT
                                try {
                                    id = comp.getString("id");
                                } catch (Exception ignored) {
                                }
                                if ((id == null || id.isEmpty()) && comp.get("Id") != null) {
                                    // fallback
                                    id = comp.get("Id").toString();
                                }

                                // Essayer d'obtenir Pos si disponible via toString fallback
                                var repr = comp.toString();
                                Matcher m = posPattern.matcher(repr);
                                if (m.find()) {
                                    ex = Double.parseDouble(m.group(1));
                                    ey = Double.parseDouble(m.group(2));
                                    ez = Double.parseDouble(m.group(3));
                                }
                            } catch (Exception excep) {
                                logger.debug("Failed to parse CompoundBinaryTag entity: {}", excep.getMessage());
                            }
                        } else {
                            // Fallback: parse the object's string representation
                            var repr = entity != null ? entity.toString() : "";
                            if (repr != null) {
                                // tenter d'extraire un id
                                var low = repr.toLowerCase();
                                if (low.contains("minecraft:armor_stand") || low.contains("armor_stand")) {
                                    id = "minecraft:armor_stand";
                                }

                                Matcher m = posPattern.matcher(repr);
                                if (m.find()) {
                                    ex = Double.parseDouble(m.group(1));
                                    ey = Double.parseDouble(m.group(2));
                                    ez = Double.parseDouble(m.group(3));
                                }
                            }
                        }

                        if (id != null && id.toLowerCase().contains("armor_stand")) {
                            // Si pas de position extraite, posons l'armor stand sur l'origine du plot
                            if (Double.isNaN(ex) || Double.isNaN(ey) || Double.isNaN(ez)) {
                                // Par défaut, place au centre du plot (position du state)
                                ex = state.getPosition().x;
                                ey = state.getPosition().y;
                                ez = state.getPosition().z;
                            } else {
                                // Les positions du schéma sont relatives; on les convertit en position monde
                                ex += state.getPosition().x;
                                ey += state.getPosition().y;
                                ez += state.getPosition().z;
                            }

                            // Créer l'entité armor stand et la positionner
                            Entity armor = new Entity(EntityType.ARMOR_STAND);
                            Pos spawnPos = new Pos(ex, ey, ez);
                            try {
                                armor.setInstance(instance, spawnPos);
                            } catch (Exception e) {
                                logger.warn("Impossible de placer l'armor stand à {} : {}", spawnPos, e.getMessage());
                            }

                            logger.info("Spawned armor stand for schematic entity at {}", spawnPos);
                        }

                    } catch (Exception e) {
                        logger.error("Erreur lors du traitement d'une entité de schéma: {}", e.getMessage(), e);
                    }
                }

                future.complete(null);
            });

            return future;
        }
        catch (Exception e) {
            System.err.println("Failed to build plot: " + e.getMessage());
            future.completeExceptionally(e);
            return future;
        }
    }
}
