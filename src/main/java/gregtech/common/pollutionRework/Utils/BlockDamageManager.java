package gregtech.common.pollutionRework.Utils;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import it.unimi.dsi.fastutil.Pair;

public class BlockDamageManager {

    private final Block masterBlock;
    private final List<Pair<Block, Integer>> blocksList;

    private BlockDamageManager(Block masterBlock, List<Pair<Block, Integer>> blocksList) {
        this.masterBlock = masterBlock;
        this.blocksList = blocksList;
    }

    @SafeVarargs
    public static BlockDamageManager setBlocksReplace(Block masterBlock, Pair<Block, Integer>... pairBlocks) {
        return new BlockDamageManager(masterBlock, Arrays.asList(pairBlocks));
    }

    public static BlockDamageManager setBlocksReplace(Block masterBlock, Block... blocks) {
        List<Pair<Block, Integer>> tempList = new ArrayList<>();
        for (Block block : blocks) {
            tempList.add(Pair.of(block, 0));
        }

        return new BlockDamageManager(masterBlock, tempList);
    }

    public Block getMasterBlock() {
        return masterBlock;
    }

    public Block getRandomBLock() {
        if (blocksList == null || blocksList.isEmpty()) {
            return Blocks.air;
        }

        int totalWeight = blocksList.stream()
            .mapToInt(Pair::second)
            .sum();

        if (totalWeight <= 0) {
            int index = XSTR_INSTANCE.nextInt(blocksList.size());
            return blocksList.get(index)
                .first();
        }

        int randomValue = XSTR_INSTANCE.nextInt(totalWeight);
        int currentWeight = 0;

        for (Pair<Block, Integer> pair : blocksList) {
            currentWeight += pair.second();

            if (randomValue < currentWeight) {
                return pair.first();
            }
        }
        return blocksList.get(blocksList.size() - 1)
            .first();
    }
}
