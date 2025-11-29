package gregtech.common.pollutionRework;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.gtnewhorizons.angelica.shadow.javax.annotation.Nullable;

import gregtech.GTMod;
import gregtech.api.util.GTChunkAssociatedData;

public class PollutionStorage extends GTChunkAssociatedData<PollutionData> {

    public PollutionStorage() {
        super("PollutionRework", PollutionData.class, 64, (byte) 0, false);
    }

    @Override
    protected void writeElement(DataOutput output, PollutionData element, World world, int chunkX, int chunkZ)
        throws IOException {
        output.writeInt(element.getAmount());
    }

    @Override
    protected PollutionData readElement(DataInput input, int version, World world, int chunkX, int chunkZ)
        throws IOException {
        if (version != 0) throw new IOException("Region file corrupted");

        PollutionData data = new PollutionData(input.readInt());
        if (data.getAmount() > 0) {
            getPollutionManager(world).getPollutedChunks()
                .add(new ChunkCoordIntPair(chunkX, chunkZ));
        }
        return data;
    }

    @Override
    protected PollutionData createElement(World world, int chunkX, int chunkZ) {
        return new PollutionData();
    }

    public void mutatePollution(World world, int x, int z, Consumer<PollutionData> mutator,
        @Nullable Set<ChunkCoordIntPair> chunks) {
        PollutionData data = get(world, x, z);
        boolean hadPollution = data.getAmount() > 0;

        mutator.accept(data);

        boolean hasPollution = data.getAmount() > 0;
        if (hasPollution != hadPollution) {
            if (chunks == null) chunks = getPollutionManager(world).getPollutedChunks();
            ChunkCoordIntPair pos = new ChunkCoordIntPair(x, z);

            if (hasPollution) chunks.add(pos);
            else chunks.remove(pos);
        }
    }

    public void setPollution(World world, ChunkCoordIntPair coord, int pollution) {
        mutatePollution(world, coord.chunkXPos, coord.chunkZPos, data -> data.setAmount(pollution), null);
    }

    public boolean isCreated(World world, ChunkCoordIntPair coord) {
        return isCreated(world.provider.dimensionId, coord.chunkXPos, coord.chunkZPos);
    }

    public PollutionData getChunkData(Chunk chunk) {
        return get(chunk.worldObj, chunk.xPosition, chunk.zPosition);
    }

    private Pollution getPollutionManager(World world) {
        return GTMod.proxy.dimensionWisePollutionRework
            .computeIfAbsent(world.provider.dimensionId, i -> new Pollution(world));
    }
}
