package gregtech.common.pollutionRework;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizons.angelica.shadow.javax.annotation.Nullable;

import gregtech.GTMod;
import gregtech.api.util.GTChunkAssociatedData;

public class PollutionStorage extends GTChunkAssociatedData<PollutionData> {

    public PollutionStorage() {
        super("PollutionRework", PollutionData.class, 64, (byte) 0, false);
    }

    @Override
    protected void writeElement(DataOutput output, PollutionData element, @NotNull World world, int chunkX, int chunkZ)
        throws IOException {
        output.writeInt(element.getAmount());
    }

    @Override
    protected PollutionData readElement(@NotNull DataInput input, int version, @NotNull World world, int chunkX,
        int chunkZ) throws IOException {
        if (version != 0) {
            throw new IOException("Region file corrupted");
        }

        int pollutionAmount = input.readInt();
        PollutionData data = new PollutionData(pollutionAmount);

        if (pollutionAmount > 0) {
            getPollutionManager(world).getPollutedChunks()
                .add(new ChunkCoordIntPair(chunkX, chunkZ));
        }

        return data;
    }

    @Override
    protected PollutionData createElement(@NotNull World world, int chunkX, int chunkZ) {
        return new PollutionData();
    }

    public void mutatePollution(World world, int chunkX, int chunkZ, Consumer<PollutionData> mutator,
        @Nullable Set<ChunkCoordIntPair> pollutedChunks) {
        PollutionData data = get(world, chunkX, chunkZ);
        boolean hadPollution = data.getAmount() > 0;

        mutator.accept(data);

        boolean hasPollution = data.getAmount() > 0;
        if (hasPollution != hadPollution) {
            Set<ChunkCoordIntPair> targetChunks = pollutedChunks != null ? pollutedChunks
                : getPollutionManager(world).getPollutedChunks();
            ChunkCoordIntPair chunkPos = new ChunkCoordIntPair(chunkX, chunkZ);

            if (hasPollution) {
                targetChunks.add(chunkPos);
            } else {
                targetChunks.remove(chunkPos);
            }
        }
    }

    public void setPollution(World world, ChunkCoordIntPair chunkCoord, int pollutionAmount) {
        mutatePollution(
            world,
            chunkCoord.chunkXPos,
            chunkCoord.chunkZPos,
            data -> data.setAmount(pollutionAmount),
            null);
    }

    public boolean isCreated(World world, ChunkCoordIntPair chunkCoord) {
        return isCreated(world.provider.dimensionId, chunkCoord.chunkXPos, chunkCoord.chunkZPos);
    }

    private Pollution getPollutionManager(World world) {
        return GTMod.proxy.dimensionWisePollutionRework
            .computeIfAbsent(world.provider.dimensionId, dimensionId -> new Pollution(world));
    }
}
