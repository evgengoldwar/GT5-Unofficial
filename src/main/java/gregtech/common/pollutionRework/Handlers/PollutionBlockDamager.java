package gregtech.common.pollutionRework.Handlers;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

import java.util.List;

import gregtech.common.pollutionRework.Utils.PollutionUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.common.pollutionRework.Utils.BlockDamageManager;
import net.minecraft.world.chunk.Chunk;

public class PollutionBlockDamager {

    private final int pollutionDamageStart;
    private final int maxAttempts;
    private final int vegetationAttemptsDivisor;
    private final List<BlockDamageManager> blockDamageManager;
    private final List<Block> listBlocksDestroy;

    public PollutionBlockDamager(int pollutionDamageStart, int maxAttempts, int vegetationAttemptsDivisor,
        List<BlockDamageManager> blockDamageManager, List<Block> listBlocksDestroy) {
        this.pollutionDamageStart = pollutionDamageStart;
        this.maxAttempts = maxAttempts;
        this.vegetationAttemptsDivisor = vegetationAttemptsDivisor;
        this.blockDamageManager = blockDamageManager;
        this.listBlocksDestroy = listBlocksDestroy;
    }

    public void applyDamageEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        if (pollution > pollutionDamageStart) {
            damageVegetation(world, chunkPos, pollution);
        }
    }

    private void damageVegetation(World world, ChunkCoordIntPair chunkPos, int pollution) {
        if (!PollutionUtils.checkIsChunkLoaded(chunkPos, world)) return;

        final int attempts = Math.min(maxAttempts, pollution / vegetationAttemptsDivisor);
        final int CHUNK_SIZE = 16;
        final int OFFSET = 2;
        final int baseX = chunkPos.chunkXPos << 4;
        final int baseZ = chunkPos.chunkZPos << 4;

        final boolean northLoaded = PollutionUtils.checkIsChunkLoaded(chunkPos.chunkXPos, chunkPos.chunkZPos - 1, world);
        final boolean southLoaded = PollutionUtils.checkIsChunkLoaded(chunkPos.chunkXPos, chunkPos.chunkZPos + 1, world);
        final boolean westLoaded = PollutionUtils.checkIsChunkLoaded(chunkPos.chunkXPos - 1, chunkPos.chunkZPos, world);
        final boolean eastLoaded = PollutionUtils.checkIsChunkLoaded(chunkPos.chunkXPos + 1, chunkPos.chunkZPos, world);

        final int minXLocal = westLoaded ? 0 : OFFSET;
        final int maxXLocal = eastLoaded ? CHUNK_SIZE - 1 : CHUNK_SIZE - 1 - OFFSET;
        final int minZLocal = northLoaded ? 0 : OFFSET;
        final int maxZLocal = southLoaded ? CHUNK_SIZE - 1 : CHUNK_SIZE - 1 - OFFSET;

        final int widthX = maxXLocal - minXLocal + 1;
        final int widthZ = maxZLocal - minZLocal + 1;

        for (int i = 0; i < attempts; i++) {
            final int localX = minXLocal + XSTR_INSTANCE.nextInt(widthX);
            final int localZ = minZLocal + XSTR_INSTANCE.nextInt(widthZ);

            int x = baseX + localX;
            int z = baseZ + localZ;

            int y = 60 + (-i + XSTR_INSTANCE.nextInt(i * 2 + 1));
            Block tBlock = world.getBlock(x, y, z);

            if (tBlock == Blocks.air) continue;

            final GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(tBlock);

            if (XSTR_INSTANCE.nextBoolean()) {
                replaceBlock(world, x, y, z, tBlock);
            } else {
                destroyBlock(world, x, y, z, tBlock, world.getBlockMetadata(x, y, z));
            }
        }
    }

    private void replaceBlock(World world, int x, int y, int z, Block tBlock) {
        if (blockDamageManager == null) return;

        int randomIndex = XSTR_INSTANCE.nextInt(blockDamageManager.size());

        BlockDamageManager bdm = blockDamageManager.get(randomIndex);

        if (bdm == null) return;

        Block masterBlock = bdm.getMasterBlock();
        Block randomBlock = bdm.getRandomBLock();

        if (tBlock == randomBlock) return;

        if (tBlock == masterBlock) {
            world.setBlock(x, y, z, randomBlock);
        }
    }

    private void destroyBlock(World world, int x, int y, int z, Block tBlock, int tMeta) {
        for (Block block : listBlocksDestroy) {
            if (tBlock == block) {
                tBlock.dropBlockAsItem(world, x, y, z, tMeta, 0);
                world.setBlockToAir(x, y, z);
            }
        }
    }
}
