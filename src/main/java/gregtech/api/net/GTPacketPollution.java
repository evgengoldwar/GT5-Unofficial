package gregtech.api.net;

import gregtech.common.pollutionWork.Api.PollutionType;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;

import com.google.common.io.ByteArrayDataInput;

import gregtech.GTMod;
import io.netty.buffer.ByteBuf;

public class GTPacketPollution extends GTPacket {

    private PollutionType type;
    private ChunkCoordIntPair chunk;
    private int pollution;

    public GTPacketPollution() {
        super();
    }

    public GTPacketPollution(PollutionType type, ChunkCoordIntPair chunk, int pollution) {
        super();
        this.type = type;
        this.chunk = chunk;
        this.pollution = pollution;
    }

    @Override
    public void encode(ByteBuf aOut) {
        aOut.writeInt(type.ordinal())
            .writeInt(chunk.chunkXPos)
            .writeInt(chunk.chunkZPos)
            .writeInt(pollution);
    }

    @Override
    public GTPacket decode(ByteArrayDataInput aData) {
        int typeOrdinal = aData.readInt();
        PollutionType type;

        try {
            type = PollutionType.values()[typeOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            GTMod.GT_FML_LOGGER.error("Received invalid pollution type ordinal: {}", typeOrdinal);
            type = PollutionType.SMOG;
        }

        ChunkCoordIntPair chunk = new ChunkCoordIntPair(aData.readInt(), aData.readInt());
        int pollution = aData.readInt();

        return new GTPacketPollution(type, chunk, pollution);
    }

    @Override
    public void process(IBlockAccess aWorld) {
        GTMod.clientProxy()
            .processChunkPollutionPacket(type, chunk, pollution);
    }

    @Override
    public byte getPacketID() {
        return GTPacketTypes.POLLUTION.id;
    }
}
