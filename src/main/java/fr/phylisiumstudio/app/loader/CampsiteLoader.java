package fr.phylisiumstudio.app.loader;

import fr.phylisiumstudio.logic.Campsite;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.anvil.AnvilLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CampsiteLoader implements IChunkLoader {
    private final AnvilLoader anvilLoader;
    private final Campsite campsite;

    public CampsiteLoader(String folder, Campsite campsite) {
        this.anvilLoader = new AnvilLoader(folder);
        this.campsite = campsite;
    }

    @Override
    public @Nullable Chunk loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        Chunk chunk = anvilLoader.loadChunk(instance, chunkX, chunkZ);

        return chunk;
    }

    @Override
    public void saveChunk(@NotNull Chunk chunk) {
        anvilLoader.saveChunk(chunk);
    }
}
