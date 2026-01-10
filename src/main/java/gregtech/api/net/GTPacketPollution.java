package gregtech.api.net;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.IBlockAccess;

import com.google.common.io.ByteArrayDataInput;

import gregtech.GTMod;
import gregtech.common.pollutionRework.Api.PollutionRegistry;
import gregtech.common.pollutionRework.Api.PollutionType;
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
        byte[] nameBytes = type.name
            .getBytes();
        aOut.writeInt(nameBytes.length);
        aOut.writeBytes(nameBytes);

        aOut.writeInt(chunk.chunkXPos)
            .writeInt(chunk.chunkZPos)
            .writeInt(pollution);
    }

    @Override
    public GTPacket decode(ByteArrayDataInput aData) {
        int nameLength = aData.readInt();
        byte[] nameBytes = new byte[nameLength];
        aData.readFully(nameBytes);

        String typeName = new String(nameBytes);

        PollutionType type = PollutionRegistry.getPollutionType(typeName);

        if (type == null) {
            GTMod.GT_FML_LOGGER.error("Received unknown pollution type: {}", typeName);
            return null;
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
