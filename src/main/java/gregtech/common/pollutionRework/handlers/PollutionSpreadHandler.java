package gregtech.common.pollutionRework.handlers;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import gregtech.api.util.GTUtility;
import gregtech.common.pollutionRework.PollutionData;
import gregtech.common.pollutionRework.PollutionStorage;

public class PollutionSpreadHandler {

    public void handlePollutionSpread(World world, ChunkCoordIntPair sourcePos, int sourcePollution,
        PollutionStorage storage) {
        ChunkCoordIntPair[] neighbors = getNeighboringChunks(sourcePos);

        for (ChunkCoordIntPair neighborPos : neighbors) {
            spreadPollutionToNeighbor(world, sourcePos, sourcePollution, neighborPos, storage);
        }
    }

    private ChunkCoordIntPair[] getNeighboringChunks(ChunkCoordIntPair center) {
        return new ChunkCoordIntPair[] { new ChunkCoordIntPair(center.chunkXPos + 1, center.chunkZPos),
            new ChunkCoordIntPair(center.chunkXPos - 1, center.chunkZPos),
            new ChunkCoordIntPair(center.chunkXPos, center.chunkZPos + 1),
            new ChunkCoordIntPair(center.chunkXPos, center.chunkZPos - 1) };
    }

    private void spreadPollutionToNeighbor(World world, ChunkCoordIntPair sourcePos, int sourcePollution,
        ChunkCoordIntPair neighborPos, PollutionStorage storage) {
        PollutionData neighborData = storage.get(world, neighborPos);
        int neighborPollution = neighborData.getAmount();

        if (neighborPollution * 6 < sourcePollution * 5) {
            int difference = (sourcePollution - neighborPollution) / 20;
            neighborPollution = GTUtility.safeInt((long) neighborPollution + difference);
            storage.setPollution(world, neighborPos, neighborPollution);
        }
    }
}
