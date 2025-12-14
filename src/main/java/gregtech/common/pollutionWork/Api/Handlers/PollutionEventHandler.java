package gregtech.common.pollutionWork.Api.Handlers;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.net.GTPacketPollution;
import gregtech.common.pollutionRework.handlers.PollutionNetworkHandler;
import gregtech.common.pollutionWork.Api.PollutionApi;
import gregtech.common.pollutionWork.Api.PollutionStorage;
import gregtech.common.pollutionWork.Api.PollutionType;

public class PollutionEventHandler {

    private static final PollutionType[] POLLUTION_TYPES = PollutionType.values();

    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch event) {
        if (event.player == null || event.player.worldObj == null) return;

        World world = event.player.worldObj;
        ChunkCoordIntPair chunkCord = new ChunkCoordIntPair(event.chunk.chunkXPos, event.chunk.chunkZPos);

        for (PollutionType type : POLLUTION_TYPES) {
            PollutionStorage storage = PollutionApi.getStorage(type);
            int pollution = 0;

            if (storage.isCreated(world, chunkCord)) {
                pollution = storage.get(world, chunkCord).getPollutionAmount();
            }

            if (pollution > 0) {
                GTValues.NW.sendToPlayer(new GTPacketPollution(type, chunkCord, pollution), event.player);
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (!event.world.isRemote) {
            for (PollutionType type : POLLUTION_TYPES) {
                PollutionApi.getStorage(type)
                    .loadAll(event.world);
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.world == null) return;
        for (PollutionType type : POLLUTION_TYPES) {
            type.getDimensionWisePollution()
                .remove(event.world.provider.dimensionId);
        }
    }
}
