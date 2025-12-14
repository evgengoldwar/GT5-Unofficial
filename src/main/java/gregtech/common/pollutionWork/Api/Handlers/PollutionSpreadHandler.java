package gregtech.common.pollutionWork.Api.Handlers;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import gregtech.common.pollutionWork.Api.PollutionData;
import gregtech.common.pollutionWork.Api.PollutionStorage;

public class PollutionSpreadHandler {

    // spotless:off
    private static final ChunkCoordIntPair[] NEIGHBOR_OFFSETS = {
        new ChunkCoordIntPair(1, 0),
        new ChunkCoordIntPair(-1, 0),
        new ChunkCoordIntPair(0, 1),
        new ChunkCoordIntPair(0, -1) };
    // spotless:on

    private static final int SPREAD_RATIO_NUMERATOR = 6;
    private static final int SPREAD_RATIO_DENOMINATOR = 5;
    private static final int SPREAD_DIVISOR = 20;

    public void handlePollutionSpread(World world, ChunkCoordIntPair sourcePos, AtomicInteger sourcePollution,
        PollutionStorage storage, Set<ChunkCoordIntPair> pollutedChunks) {

        int totalSpread = 0;

        for (ChunkCoordIntPair offset : NEIGHBOR_OFFSETS) {
            ChunkCoordIntPair neighborPos = new ChunkCoordIntPair(
                sourcePos.chunkXPos + offset.chunkXPos,
                sourcePos.chunkZPos + offset.chunkZPos);

            totalSpread += spreadPollutionToNeighbor(
                world,
                sourcePos,
                sourcePollution.get(),
                neighborPos,
                storage,
                pollutedChunks);
        }

        if (totalSpread > 0) {
            sourcePollution.addAndGet(-totalSpread);
        }
    }

    private int spreadPollutionToNeighbor(World world, ChunkCoordIntPair sourcePos, int sourcePollution,
        ChunkCoordIntPair neighborPos, PollutionStorage storage, Set<ChunkCoordIntPair> pollutedChunks) {

        PollutionData neighborData = storage.get(world, neighborPos);
        int neighborPollution = neighborData.getPollutionAmount();

        if (shouldSpreadPollution(sourcePollution, neighborPollution)) {
            int difference = calculateSpreadDifference(sourcePollution, neighborPollution);
            if (difference > 0) {
                storage.mutatePollution(
                    world,
                    neighborPos.chunkXPos,
                    neighborPos.chunkZPos,
                    data -> data.changePollutionAmount(difference),
                    pollutedChunks);
                return difference;
            }
        }
        return 0;
    }

    private boolean shouldSpreadPollution(long sourcePollution, long neighborPollution) {
        return neighborPollution * SPREAD_RATIO_NUMERATOR < sourcePollution * SPREAD_RATIO_DENOMINATOR;
    }

    private int calculateSpreadDifference(int sourcePollution, int neighborPollution) {
        return (sourcePollution - neighborPollution) / SPREAD_DIVISOR;
    }
}
