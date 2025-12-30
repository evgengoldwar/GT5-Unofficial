package gregtech.common.pollutionRework.Handlers;

import gregtech.common.pollutionRework.Utils.PollutionUtils;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

public class PollutionBiomeChangeHandler {

    public static void changeChunkBiome(World world, ChunkCoordIntPair chunkPos, BiomeGenBase biome) {

        if (!PollutionUtils.checkIsChunkLoaded(chunkPos, world)) {
            return;
        }

        if (biome == null) return;

        Chunk chunk = world.getChunkFromChunkCoords(chunkPos.chunkXPos, chunkPos.chunkZPos);

        if (chunk == null) return;

        int centerX = (chunkPos.chunkXPos << 4) + 8;
        int centerZ = (chunkPos.chunkZPos << 4) + 8;
        BiomeGenBase currentBiome = world.getBiomeGenForCoords(centerX, centerZ);

        if (currentBiome == biome) return;

        setChunkBiome(world, chunk, biome);

        chunk.setChunkModified();
    }

    private static void setChunkBiome(World world, Chunk chunk, BiomeGenBase biome) {
        byte biomeId = (byte) biome.biomeID;
        byte[] biomeArray = chunk.getBiomeArray();

        if (biomeArray == null) {
            biomeArray = new byte[256];
        }

        java.util.Arrays.fill(biomeArray, biomeId);

        chunk.setBiomeArray(biomeArray);

        world.getWorldChunkManager().cleanupCache();
    }
}
