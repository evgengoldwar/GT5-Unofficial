package gregtech.mixin.mixins.late.biomesoplenty;

import org.spongepowered.asm.mixin.Mixin;

import biomesoplenty.client.render.blocks.FoliageRenderer;

@Mixin(FoliageRenderer.class)
public class MixinFoliageRendererPollution {

    // @ModifyExpressionValue(
    // method = "renderCrossedSquares",
    // remap = false,
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/block/Block;func_149720_d(Lnet/minecraft/world/IBlockAccess;III)I",
    // remap = false))
    // private int gt5u$pollutionCrossedSquares(int color, Block block, int blockX, int blockY, int blockZ,
    // RenderBlocks renderer) {
    // ColorOverrideTypeRework type = Pollution.blockVine.matchesID(block);
    // if (type == null) return color;
    // return type.getColor(color, blockX, blockZ);
    // }
}
