package gregtech.common.pollutionRework.Data;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

import gregtech.common.pollutionRework.Api.Pollution;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizons.angelica.shadow.javax.annotation.Nullable;

import gregtech.api.util.GTChunkAssociatedData;
import gregtech.common.pollutionRework.Api.PollutionType;

public class PollutionStorage extends GTChunkAssociatedData<PollutionData> {

    private final PollutionType pollutionType;

    public PollutionStorage(PollutionType pollutionType) {
        super("Pollution_" + pollutionType.name, PollutionData.class, 64, (byte) 0, false);
        this.pollutionType = pollutionType;
    }

    @Override
    protected void writeElement(DataOutput output, PollutionData element, @NotNull World world, int chunkX, int chunkZ)
        throws IOException {
        output.writeInt(element.getPollutionAmount());
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
        boolean hadPollution = data.getPollutionAmount() > 0;

        mutator.accept(data);

        boolean hasPollution = data.getPollutionAmount() > 0;
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

    public boolean isCreated(World world, ChunkCoordIntPair chunkCord) {
        return isCreated(world.provider.dimensionId, chunkCord.chunkXPos, chunkCord.chunkZPos);
    }

    private Pollution getPollutionManager(World world) {
        return pollutionType.dimensionWisePollution
            .computeIfAbsent(world.provider.dimensionId, dimensionId -> pollutionType.createPollutionInstance());
    }
}
