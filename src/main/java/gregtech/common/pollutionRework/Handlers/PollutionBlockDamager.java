package gregtech.common.pollutionRework.Handlers;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;


public class PollutionBlockDamager {

    private final int pollutionDamageStart;
    private final int maxAttempts;
    private final int vegetationAttemptsDivisor;
    private final List<Pair<Block, Block>> listPairBlocksReplace;
    private final List<Block> listBlocksDestroy;

    public PollutionBlockDamager(int pollutionDamageStart, int maxAttempts, int vegetationAttemptsDivisor,
        List<Pair<Block, Block>> listPairBlocksReplace, List<Block> listBlocksDestroy) {
        this.pollutionDamageStart = pollutionDamageStart;
        this.maxAttempts = maxAttempts;
        this.vegetationAttemptsDivisor = vegetationAttemptsDivisor;
        this.listPairBlocksReplace = listPairBlocksReplace;
        this.listBlocksDestroy = listBlocksDestroy;
    }

    public void applyDamageEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        if (pollution > pollutionDamageStart) {
            damageVegetation(world, chunkPos, pollution);
        }
    }

    private void damageVegetation(World world, ChunkCoordIntPair chunkPos, int pollution) {
        int attempts = Math.min(maxAttempts, pollution / vegetationAttemptsDivisor);
        final int CHUNK_SIZE = 16;

        for (int i = 0; i < attempts; i++) {
            int baseX = chunkPos.chunkXPos << 4;
            int baseZ = chunkPos.chunkZPos << 4;
            int x = baseX + XSTR_INSTANCE.nextInt(CHUNK_SIZE);
            int y = 60 + (-i + XSTR_INSTANCE.nextInt(i * 2 + 1));
            int z = baseZ + XSTR_INSTANCE.nextInt(CHUNK_SIZE);
            Block tBlock = world.getBlock(x, y, z);
            int tMeta = world.getBlockMetadata(x, y, z);

            if (world.isRemote) return;

            if (tBlock == Blocks.air) continue;

            if (XSTR_INSTANCE.nextBoolean()) {
                replaceBlock(world, x, y, z, tBlock);
            } else {
                destroyBlock(world, x, y, z, tBlock, tMeta);
            }
        }
    }

    private void replaceBlock(World world, int x, int y, int z, Block tBlock) {
        for (Pair<Block, Block> blocks : listPairBlocksReplace) {
            Block leftBlock = blocks.first();
            Block rightBlock = blocks.second();

            if (tBlock == rightBlock) return;

            if (tBlock == leftBlock) {
                world.setBlock(x, y, z, rightBlock);
            }
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
