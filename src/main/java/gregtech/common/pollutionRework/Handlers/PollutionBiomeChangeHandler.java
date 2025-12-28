package gregtech.common.pollutionRework.Handlers;

import gregtech.common.pollutionRework.Utils.PollutionUtils;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

public class PollutionBiomeChangeHandler {

    public static void changeChunkBiome(World world, ChunkCoordIntPair chunkPos, BiomeGenBase biome) {
        if (!PollutionUtils.checkIsChunkLoaded(chunkPos, world)) {
            return;
        }

        if (biome == null) return;

        Chunk chunk = world.getChunkFromBlockCoords(chunkPos.chunkXPos, chunkPos.chunkZPos);

        BiomeGenBase currentBiome = chunk.getBiomeGenForWorldCoords(7, 7, world.getWorldChunkManager());

        if (currentBiome == biome) return;

        setChunkBiome(chunk, biome);
        updateChunkVisuals(world, chunkPos);
    }

    private static void setChunkBiome(Chunk chunk, BiomeGenBase biome) {
        byte biomeId = (byte) biome.biomeID;
        byte[] biomeArray = chunk.getBiomeArray();

        if (biomeArray == null) {
            biomeArray = new byte[256];
        }

        for (int i = 0; i < 256; i++) {
            biomeArray[i] = biomeId;
        }

        chunk.setBiomeArray(biomeArray);
        chunk.setChunkModified();
    }

    private static void updateChunkVisuals(World world, ChunkCoordIntPair chunkPos) {
        if (world.isRemote) return;

        updateBiomeDependentBlocks(world, chunkPos);
        sendChunkUpdatePacket(world, chunkPos);
    }

    private static void updateBiomeDependentBlocks(World world, ChunkCoordIntPair chunkPos) {
        int worldXStart = chunkPos.chunkXPos << 4;
        int worldZStart = chunkPos.chunkZPos << 4;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = worldXStart + x;
                int worldZ = worldZStart + z;

                updateFoliageColor(world, worldX, worldZ);
            }
        }
    }

    private static void updateFoliageColor(World world, int x, int z) {
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                world.markBlockRangeForRenderUpdate(x + dx, 0, z + dz, x + dx, 255, z + dz);
            }
        }
    }

    private static void sendChunkUpdatePacket(World world, ChunkCoordIntPair chunkPos) {
        Chunk chunk = world.getChunkFromChunkCoords(chunkPos.chunkXPos, chunkPos.chunkZPos);
        if (chunk != null) {
            world.markBlockForUpdate(chunkPos.chunkXPos << 4, 64, chunkPos.chunkZPos << 4);
        }
    }
}
