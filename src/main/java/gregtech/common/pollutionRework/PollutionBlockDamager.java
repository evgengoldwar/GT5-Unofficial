package gregtech.common.pollutionRework;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class PollutionBlockDamager {

    private static final Set<Block> HARVESTABLE_PLANTS = Stream
        .of(
            Blocks.reeds,
            Blocks.vine,
            Blocks.waterlily,
            Blocks.wheat,
            Blocks.cactus,
            Blocks.melon_block,
            Blocks.melon_stem,
            Blocks.red_flower,
            Blocks.yellow_flower,
            Blocks.carrots,
            Blocks.potatoes,
            Blocks.pumpkin,
            Blocks.pumpkin_stem,
            Blocks.cocoa)
        .collect(Collectors.toSet());

    private static final Set<Block> IMMUNE_BLOCKS = Stream.of(Blocks.air, Blocks.stone, Blocks.sand, Blocks.deadbush)
        .collect(Collectors.toSet());

    private static final Set<Block> LEAF_BLOCKS = Stream.of(Blocks.leaves, Blocks.leaves2)
        .collect(Collectors.toSet());

    private static final Set<Block> WEATHER_AFFECTED_BLOCKS = Stream.of(Blocks.gravel, Blocks.cobblestone)
        .collect(Collectors.toSet());

    public static void damageBlock(World world, int x, int y, int z, boolean sourRain) {
        if (world.isRemote) return;

        Block block = world.getBlock(x, y, z);

        if (isImmuneBlock(block)) return;

        handleBlockDamage(world, x, y, z, block, world.getBlockMetadata(x, y, z), sourRain);
    }

    private static boolean isImmuneBlock(Block block) {
        return IMMUNE_BLOCKS.contains(block);
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
        return LEAF_BLOCKS.contains(block) || block.getMaterial() == Material.leaves;
    }

    private static boolean isHarvestablePlant(Block block) {
        return HARVESTABLE_PLANTS.contains(block) || block.getMaterial() == Material.cactus;
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
        return WEATHER_AFFECTED_BLOCKS.contains(block);
    }
}
