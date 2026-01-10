package gregtech.common.pollutionRework.Api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.gtnewhorizon.gtnhlib.capability.Capabilities;

import gregtech.GTMod;
import gregtech.api.interfaces.ICleanroom;
import gregtech.api.interfaces.ICleanroomReceiver;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.pollutionRework.Data.PollutionStorage;

public class PollutionApi {

    // region Add Pollution
    public static void addPollution(Chunk chunk, int pollution, PollutionType pollutionType) {
        addPollution(chunk.worldObj, chunk.xPosition, chunk.zPosition, pollution, pollutionType);
    }

    public static void addPollution(World world, int chunkX, int chunkZ, int pollution, PollutionType pollutionType) {
        if (pollution == 0 || world.isRemote) return;
        getStorage(pollutionType)
            .mutatePollution(world, chunkX, chunkZ, data -> data.changePollutionAmount(pollution), null);
    }

    public static void addPollution(IGregTechTileEntity te, int pollution, PollutionType pollutionType) {
        addPollution((TileEntity) te, pollution, pollutionType);
    }

    public static void addPollution(TileEntity te, int pollution, PollutionType pollutionType) {
        if (pollution == 0 || te.getWorldObj().isRemote) return;

        if (pollution > 0) {
            handleCleanroomPollution(te);
        }

        addPollution(te.getWorldObj(), te.xCoord >> 4, te.zCoord >> 4, pollution, pollutionType);
    }
    // endregion

    // region Get Pollution
    public static int getPollution(Chunk chunk, PollutionType type) {
        return getPollution(chunk.worldObj, chunk.xPosition, chunk.zPosition, type);
    }

    public static int getPollution(World world, int chunkX, int chunkZ, PollutionType type) {
        if (world.isRemote) {
            return GTMod.clientProxy().mPollutionRenderer.getKnownPollution(chunkX << 4, chunkZ << 4);
        }
        return getStorage(type).get(world, chunkX, chunkZ)
            .getPollutionAmount();
    }
    // endregion

    // region Other Methods
    public static boolean hasPollution(Chunk chunk, PollutionType type) {
        return getStorage(type).isCreated(chunk.worldObj, chunk.getChunkCoordIntPair()) && getStorage(type).get(chunk)
            .getPollutionAmount() > 0;
    }

    private static void handleCleanroomPollution(TileEntity te) {
        ICleanroomReceiver receiver = Capabilities.getCapability(te, ICleanroomReceiver.class);
        if (receiver != null) {
            ICleanroom cleanroom = receiver.getCleanroom();
            if (cleanroom != null && cleanroom.isValidCleanroom()) {
                cleanroom.pollute();
            }
        }
    }

    public static PollutionStorage getStorage(PollutionType type) {
        return type.storage;
    }
    // endregion
}
