package gregtech.mixin.mixins.early.minecraft.pollution;

import net.minecraft.client.renderer.RenderBlocks;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(RenderBlocks.class)
public class MixinRenderBlocks_PollutionWithoutOptifine {

    // @ModifyExpressionValue(
    // method = "renderStandardBlock",
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/block/Block;colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I"))
    // private int gt5u$pollutionStandardBlock(int color, Block block, int blockX, int blockY, int blockZ) {
    // return PollutionRendererRegistry.getInstance().getColorForBlock(block, color, blockX, blockZ);
    // }
    //
    // @ModifyExpressionValue(
    // method = "renderBlockLiquid",
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/block/Block;colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I"))
    // private int gt5u$pollutionBlockLiquid(int color, Block block, int blockX, int blockY, int blockZ) {
    // return PollutionRendererRegistry.getInstance().getColorForBlock(block, color, blockX, blockZ);
    // }
    //
    // @ModifyExpressionValue(
    // method = "renderBlockDoublePlant",
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/block/BlockDoublePlant;colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I"))
    // private int gt5u$pollutionBlockDoublePlant(int color, BlockDoublePlant block, int blockX, int blockY, int blockZ)
    // {
    // return PollutionRendererRegistry.getInstance().getColorForBlock(block, color, blockX, blockZ);
    // }
    //
    // @ModifyExpressionValue(
    // method = "renderCrossedSquares",
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/block/Block;colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I"))
    // private int gt5u$pollutionCrossedSquares(int color, Block block, int blockX, int blockY, int blockZ) {
    // return PollutionRendererRegistry.getInstance().getColorForBlock(block, color, blockX, blockZ);
    // }
    //
    // @ModifyExpressionValue(
    // method = "renderBlockVine",
    // at = @At(
    // value = "INVOKE",
    // target = "Lnet/minecraft/block/Block;colorMultiplier(Lnet/minecraft/world/IBlockAccess;III)I"))
    // private int gt5u$pollutionBlockVine(int color, Block block, int blockX, int blockY, int blockZ) {
    // return PollutionRendererRegistry.getInstance().getColorForBlock(block, color, blockX, blockZ);
    // }
}
