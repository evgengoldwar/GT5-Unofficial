package gregtech.common.pollutionRework;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class PollutionBlockDamager {

    private static final Block[] HARVESTABLE_PLANTS = { Blocks.reeds, Blocks.vine, Blocks.waterlily, Blocks.wheat,
        Blocks.cactus, Blocks.melon_block, Blocks.melon_stem, Blocks.red_flower, Blocks.yellow_flower, Blocks.carrots,
        Blocks.potatoes, Blocks.pumpkin, Blocks.pumpkin_stem, Blocks.cocoa };

    public static void damageBlock(World world, int x, int y, int z, boolean sourRain) {
        if (world.isRemote) return;

        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        if (isImmuneBlock(block)) return;

        handleBlockDamage(world, x, y, z, block, meta, sourRain);
    }

    private static boolean isImmuneBlock(Block block) {
        return block == Blocks.air || block == Blocks.stone || block == Blocks.sand || block == Blocks.deadbush;
    }

    private static void handleBlockDamage(World world, int x, int y, int z, Block block, int meta, boolean sourRain) {
        if (isLeafBlock(block)) {
            world.setBlockToAir(x, y, z);
        } else if (isHarvestablePlant(block)) {
            harvestAndReplace(world, x, y, z, block, meta);
        } else if (block == Blocks.tallgrass) {
            world.setBlock(x, y, z, Blocks.deadbush);
        } else if (block == Blocks.grass || block.getMaterial() == Material.grass) {
            world.setBlock(x, y, z, Blocks.dirt);
        } else if (block == Blocks.farmland || block == Blocks.dirt) {
            world.setBlock(x, y, z, Blocks.sand);
        } else if (block == Blocks.mossy_cobblestone) {
            world.setBlock(x, y, z, Blocks.cobblestone);
        } else if (block == Blocks.sapling || block.getMaterial() == Material.plants) {
            world.setBlock(x, y, z, Blocks.deadbush);
        }

        handleSourRainEffect(world, x, y, z, block, sourRain);
    }

    private static boolean isLeafBlock(Block block) {
        return block == Blocks.leaves || block == Blocks.leaves2 || block.getMaterial() == Material.leaves;
    }

    private static boolean isHarvestablePlant(Block block) {
        for (Block plant : HARVESTABLE_PLANTS) {
            if (block == plant) return true;
        }
        return block.getMaterial() == Material.cactus;
    }

    private static void harvestAndReplace(World world, int x, int y, int z, Block block, int meta) {
        block.dropBlockAsItem(world, x, y, z, meta, 0);
        world.setBlockToAir(x, y, z);
    }

    private static void handleSourRainEffect(World world, int x, int y, int z, Block block, boolean sourRain) {
        if (sourRain && world.isRaining()
            && isWeatherAffectedBlock(block)
            && world.getBlock(x, y + 1, z) == Blocks.air
            && world.canBlockSeeTheSky(x, y, z)) {

            if (block == Blocks.cobblestone) {
                world.setBlock(x, y, z, Blocks.gravel);
            } else if (block == Blocks.gravel) {
                world.setBlock(x, y, z, Blocks.sand);
            }
        }
    }

    private static boolean isWeatherAffectedBlock(Block block) {
        return block == Blocks.gravel || block == Blocks.cobblestone;
    }
}
