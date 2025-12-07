package gregtech.common.pollutionWork.handlers;

import net.minecraft.world.World;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import gregtech.api.enums.GTValues;
import gregtech.api.net.GTPacketPollution;
import gregtech.common.pollutionRework.handlers.PollutionNetworkHandler;
import gregtech.common.pollutionWork.api.PollutionApi;
import gregtech.common.pollutionWork.api.PollutionStorage;
import gregtech.common.pollutionWork.api.PollutionType;

public class PollutionEventHandler {

    private static final PollutionType[] POLLUTION_TYPES = PollutionType.values();

    @SubscribeEvent
    public void onChunkWatch(ChunkWatchEvent.Watch event) {
        if (event.player == null || event.player.worldObj == null) return;

        World world = event.player.worldObj;
        for (PollutionType type : POLLUTION_TYPES) {
            PollutionStorage storage = PollutionApi.getStorage(type);
            if (storage.isCreated(world, event.chunk)) {
                int pollution = storage.get(world, event.chunk)
                    .getPollutionAmount();
                if (PollutionNetworkHandler.shouldSendUpdate(pollution)) {
                    GTValues.NW.sendToPlayer(new GTPacketPollution(event.chunk, pollution), event.player);
                }
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
