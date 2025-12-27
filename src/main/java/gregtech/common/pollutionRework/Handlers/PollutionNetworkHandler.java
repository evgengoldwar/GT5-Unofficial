package gregtech.common.pollutionRework.Handlers;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import cpw.mods.fml.common.network.NetworkRegistry;
import gregtech.api.enums.GTValues;
import gregtech.api.net.GTPacketPollution;
import gregtech.common.pollutionRework.Api.PollutionType;

public class PollutionNetworkHandler {

    private static final int POLLUTION_PACKET_MIN_VALUE = 1000;
    private static final int CHUNK_TO_BLOCK_SHIFT = 4;
    private static final int DEFAULT_Y_LEVEL = 64;
    private static final int NETWORK_RANGE = 256;

    public synchronized static void sendPollutionUpdate(World world, ChunkCoordIntPair chunkPos, int pollution,
        PollutionType pollutionType) {
        if (!shouldSendUpdate(pollution)) {
            return;
        }

        NetworkRegistry.TargetPoint point = createTargetPoint(world, chunkPos);
        GTValues.NW.sendToAllAround(new GTPacketPollution(pollutionType, chunkPos, pollution), point);
    }

    public static boolean shouldSendUpdate(int pollution) {
        return pollution > POLLUTION_PACKET_MIN_VALUE;
    }

    private static NetworkRegistry.TargetPoint createTargetPoint(World world, ChunkCoordIntPair chunkPos) {
        int dimensionId = world.provider.dimensionId;
        int centerX = chunkPos.chunkXPos << CHUNK_TO_BLOCK_SHIFT;
        int centerZ = chunkPos.chunkZPos << CHUNK_TO_BLOCK_SHIFT;

        return new NetworkRegistry.TargetPoint(dimensionId, centerX, DEFAULT_Y_LEVEL, centerZ, NETWORK_RANGE);
    }
}
