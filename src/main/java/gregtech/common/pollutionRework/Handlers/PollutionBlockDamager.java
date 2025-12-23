package gregtech.common.pollutionRework.Handlers;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.common.pollutionRework.Utils.BlockDamageManager;

public class PollutionBlockDamager {

    private final int pollutionDamageStart;
    private final int maxAttempts;
    private final int vegetationAttemptsDivisor;
    private final BlockDamageManager blockDamageManager;
    private final List<Block> listBlocksDestroy;

    public PollutionBlockDamager(int pollutionDamageStart, int maxAttempts, int vegetationAttemptsDivisor,
        BlockDamageManager blockDamageManager, List<Block> listBlocksDestroy) {
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

            final GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(tBlock);

            if (XSTR_INSTANCE.nextBoolean()) {
                replaceBlock(world, x, y, z, tBlock);
            } else {
                destroyBlock(world, x, y, z, tBlock, tMeta);
            }
        }
    }

    private void replaceBlock(World world, int x, int y, int z, Block tBlock) {
        if (blockDamageManager == null) return;

        Block masterBlock = blockDamageManager.getMasterBlock();
        Block randomBlock = blockDamageManager.getRandomBLock();

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
