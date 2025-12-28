package gregtech.common.pollutionRework.Utils;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class PollutionUtils {
    public static boolean checkIsChunkLoaded(ChunkCoordIntPair chunkPos, World world) {
        int x = chunkPos.chunkXPos;
        int z = chunkPos.chunkZPos;

        if (!world.getChunkProvider().chunkExists(x, z)) {
            return false;
        }

        Chunk chunk = world.getChunkFromBlockCoords(x, z);
        if (chunk == null) return false;

        return chunk.isChunkLoaded;
    }

    public static boolean checkIsChunkLoaded(int chunkXPos, int chunkZPos, World world) {
        if (!world.getChunkProvider().chunkExists(chunkXPos, chunkZPos)) {
            return false;
        }

        Chunk chunk = world.getChunkFromBlockCoords(chunkXPos, chunkZPos);
        if (chunk == null) return false;

        return chunk.isChunkLoaded;
    }
}
