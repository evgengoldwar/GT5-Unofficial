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
    private final List<Pair<Block, Integer>> listBlocks;

    private BlockDamageManager(Block masterBlock, List<Pair<Block, Integer>> listBlocks) {
        this.masterBlock = masterBlock;
        this.listBlocks = listBlocks;
    }

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
        if (listBlocks == null || listBlocks.isEmpty()) {
            return Blocks.air;
        }

        int totalWeight = listBlocks.stream()
            .mapToInt(Pair::second)
            .sum();

        if (totalWeight <= 0) {
            int index = XSTR_INSTANCE.nextInt(listBlocks.size());
            return listBlocks.get(index)
                .first();
        }

        int randomValue = XSTR_INSTANCE.nextInt(totalWeight);
        int currentWeight = 0;

        for (Pair<Block, Integer> pair : listBlocks) {
            currentWeight += pair.second();

            if (randomValue < currentWeight) {
                return pair.first();
            }
        }
        return listBlocks.get(listBlocks.size() - 1)
            .first();
    }
}
