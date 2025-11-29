package gregtech.common.pollutionRework.handlers;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import gregtech.api.util.GTUtility;
import gregtech.common.pollutionRework.PollutionData;
import gregtech.common.pollutionRework.PollutionStorage;

public class PollutionSpreadHandler {
    private static final ChunkCoordIntPair[] NEIGHBOR_OFFSETS = {
        new ChunkCoordIntPair(1, 0),
        new ChunkCoordIntPair(-1, 0),
        new ChunkCoordIntPair(0, 1),
        new ChunkCoordIntPair(0, -1)
    };

    public void handlePollutionSpread(World world, ChunkCoordIntPair sourcePos, int sourcePollution, PollutionStorage storage) {
        for (ChunkCoordIntPair offset : NEIGHBOR_OFFSETS) {
            ChunkCoordIntPair neighborPos = new ChunkCoordIntPair(sourcePos.chunkXPos + offset.chunkXPos, sourcePos.chunkZPos + offset.chunkZPos);
            spreadPollutionToNeighbor(world, sourcePos, sourcePollution, neighborPos, storage);
        }
    }

    private void spreadPollutionToNeighbor(World world, ChunkCoordIntPair sourcePos, int sourcePollution, ChunkCoordIntPair neighborPos, PollutionStorage storage) {
        PollutionData neighborData = storage.get(world, neighborPos);
        int neighborPollution = neighborData.getAmount();

        if (neighborPollution * 6 < sourcePollution * 5) {
            int difference = (sourcePollution - neighborPollution) / 20;
            neighborPollution = GTUtility.safeInt((long) neighborPollution + difference);
            storage.setPollution(world, neighborPos, neighborPollution);
        }
    }
}
