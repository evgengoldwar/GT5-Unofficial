package gregtech.common.pollutionWork.ApiRenders;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MathHelper;

import java.util.Arrays;

public class ClientPollutionMap {

    private static final byte RADIUS = 24;
    private static final byte DISTANCE_RELOAD_MAP = 5;
    private static final byte SIZE = RADIUS * 2 + 1;
    private static final int POLLUTION_DIVISOR = 225;
    private static final int MAX_POLLUTION = Short.MAX_VALUE;

    private int centerChunkX;
    private int centerChunkZ;
    private int dimension;
    private short[][] chunkPollutionData;
    private boolean needsRebuild = true;

    public void markDirty() {
        needsRebuild = true;
    }

    private void initialize(int playerChunkX, int playerChunkZ, int dimension) {
        needsRebuild = false;
        chunkPollutionData = new short[SIZE][SIZE];
        centerChunkX = playerChunkX;
        centerChunkZ = playerChunkZ;
        this.dimension = dimension;
    }

    public void updateChunkPollution(int chunkX, int chunkZ, int pollution) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null || player.worldObj == null) return;

        int playerChunkX = MathHelper.floor_double(player.posX) >> 4;
        int playerChunkZ = MathHelper.floor_double(player.posZ) >> 4;

        if (needsRebuild || dimension != player.dimension) {
            initialize(playerChunkX, playerChunkZ, player.dimension);
        }

        if (Math.abs(centerChunkX - playerChunkX) > DISTANCE_RELOAD_MAP
            || Math.abs(centerChunkZ - playerChunkZ) > DISTANCE_RELOAD_MAP) {
            shiftCenter(playerChunkX, playerChunkZ);
        }

        int relativeX = chunkX - centerChunkX + RADIUS;
        int relativeZ = chunkZ - centerChunkZ + RADIUS;

        if (relativeX >= SIZE || relativeX < 0 || relativeZ >= SIZE || relativeZ < 0) {
            return;
        }

        short currentValue = chunkPollutionData[relativeX][relativeZ];
        if (pollution > 0 || currentValue == 0) {
            int processedPollution = Math.min(Math.max(pollution / POLLUTION_DIVISOR, 0), MAX_POLLUTION);
            chunkPollutionData[relativeX][relativeZ] = (short) processedPollution;
        }
    }

    public int getInterpolatedPollution(double worldX, double worldZ) {
        if (needsRebuild || chunkPollutionData == null) return 0;

        int chunkX = MathHelper.floor_double(worldX) >> 4;
        int chunkZ = MathHelper.floor_double(worldZ) >> 4;

        int localX = MathHelper.floor_double(worldX) & 15;
        int localZ = MathHelper.floor_double(worldZ) & 15;

        int matrixX = chunkX - centerChunkX + RADIUS;
        int matrixZ = chunkZ - centerChunkZ + RADIUS;

        if (matrixX < 0) matrixX = 0;
        if (matrixX >= SIZE - 1) matrixX = SIZE - 2;
        if (matrixZ < 0) matrixZ = 0;
        if (matrixZ >= SIZE - 1) matrixZ = SIZE - 2;

        int pollution00 = chunkPollutionData[matrixX][matrixZ] & 0xFFFF;
        int pollution10 = chunkPollutionData[matrixX + 1][matrixZ] & 0xFFFF;
        int pollution01 = chunkPollutionData[matrixX][matrixZ + 1] & 0xFFFF;
        int pollution11 = chunkPollutionData[matrixX + 1][matrixZ + 1] & 0xFFFF;

        float dx = localX / 16.0f;
        float dz = localZ / 16.0f;

        float interpolated = pollution00 * (1 - dx) * (1 - dz) + pollution10 * dx * (1 - dz)
            + pollution01 * (1 - dx) * dz
            + pollution11 * dx * dz;

        return (int) (interpolated * POLLUTION_DIVISOR);
    }

    private void shiftCenter(int newChunkX, int newChunkZ) {
        int deltaX = newChunkX - centerChunkX;
        int deltaZ = newChunkZ - centerChunkZ;
        boolean[] emptyRows = new boolean[SIZE];

        if (deltaX != 0) {
            shiftHorizontal(deltaX, emptyRows);
        }

        if (deltaZ != 0) {
            shiftVertical(deltaZ, emptyRows);
        }

        centerChunkX = newChunkX;
        centerChunkZ = newChunkZ;
    }

    private void shiftHorizontal(int deltaX, boolean[] emptyRows) {
        if (deltaX > 0) {
            Arrays.setAll(chunkPollutionData, x -> {
                int sourceX = x + deltaX;
                if (sourceX < SIZE) {
                    return chunkPollutionData[sourceX].clone();
                } else {
                    emptyRows[x] = true;
                    return new short[SIZE];
                }
            });
        } else {
            for (int x = SIZE - 1; x >= 0; x--) {
                int sourceX = x + deltaX;
                if (sourceX >= 0) {
                    chunkPollutionData[x] = chunkPollutionData[sourceX].clone();
                } else {
                    chunkPollutionData[x] = new short[SIZE];
                    emptyRows[x] = true;
                }
            }
        }
    }

    private void shiftVertical(int deltaZ, boolean[] emptyRows) {
        if (deltaZ > 0) {
            Arrays.stream(chunkPollutionData)
                .forEach(row -> {
                    if (row != null) {
                        System.arraycopy(row, deltaZ, row, 0, SIZE - deltaZ);
                        Arrays.fill(row, SIZE - deltaZ, SIZE, (short) 0);
                    }
                });
        } else {
            int absDeltaZ = -deltaZ;
            Arrays.stream(chunkPollutionData)
                .forEach(row -> {
                    if (row != null) {
                        System.arraycopy(row, 0, row, absDeltaZ, SIZE - absDeltaZ);
                        Arrays.fill(row, 0, absDeltaZ, (short) 0);
                    }
                });
        }
    }
}
