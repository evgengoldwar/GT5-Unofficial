package gregtech.common.pollutionRework.Handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import gregtech.GTMod;
import gregtech.api.hazards.HazardProtection;

public class PollutionEffectHandler {

    private final int SMOG_DURATION_DIVISOR = 1000;
    private final int SMOG_AMPLIFIER_DIVISOR = 400000;
    private final int POISON_HUNGER_DIVISOR = 500000;
    private final int POISON_CONFUSION_DIVISOR = 2000;
    private final int POISON_POISON_DIVISOR = 4000;
    private final int MAX_DURATION = 1000;
    private final int CHUNK_HEIGHT = 256;
    private final int CHUNK_SIZE = 16;
    private final List<Potion> potionList = new ArrayList<>();

    public PollutionEffectHandler(List<Potion> potionList) {
        this.potionList.addAll(potionList);
    }

    public void applyPotionEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        AxisAlignedBB chunkBounds = createChunkBoundingBox(chunkPos);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, chunkBounds);

        entities.stream()
            .filter(this::shouldApplyEffects)
            .forEach(entity -> processEntityEffects(entity, pollution));
    }

    private AxisAlignedBB createChunkBoundingBox(ChunkCoordIntPair chunkPos) {
        int minX = chunkPos.chunkXPos << 4;
        int minZ = chunkPos.chunkZPos << 4;
        return AxisAlignedBB.getBoundingBox(minX, 0, minZ, minX + CHUNK_SIZE, CHUNK_HEIGHT, minZ + CHUNK_SIZE);
    }

    private boolean shouldApplyEffects(EntityLivingBase entity) {
        return !(entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).capabilities.isCreativeMode);
    }

    private void processEntityEffects(EntityLivingBase entity, int pollution) {
        if (!HazardProtection.isWearingFullGasHazmat(entity) && pollution > GTMod.proxy.mPollutionPoisonLimit) {
            int duration = 1;
            int amplifier = 1;
            Potion randomPotion = getRandomPotion(potionList);

            if (randomPotion != null) {
                entity.addPotionEffect(new PotionEffect(randomPotion.id, duration, amplifier));
            }
        }
    }

    private Potion getRandomPotion(List<Potion> list) {
        if (list == null || list.isEmpty()) return null;

        List<Potion> copy = new ArrayList<>(list);
        Collections.shuffle(copy);
        return copy.get(0);
    }

    // private void applyNegativeEffects(EntityLivingBase entity, int pollution) {
    // int duration = Math.min(pollution / SMOG_DURATION_DIVISOR, MAX_DURATION);
    // int amplifier = pollution / SMOG_AMPLIFIER_DIVISOR;
    //
    // PotionEffect effect = switch (XSTR_INSTANCE.nextInt(3)) {
    // case 0 -> new PotionEffect(Potion.digSlowdown.id, duration, amplifier);
    // case 1 -> new PotionEffect(Potion.weakness.id, duration, amplifier);
    // case 2 -> new PotionEffect(Potion.moveSlowdown.id, duration, amplifier);
    // default -> null;
    // };
    //
    // entity.addPotionEffect(effect);
    // }
    //
    // private void applyPoisonEffects(EntityLivingBase entity, int pollution) {
    // PotionEffect effect = switch (XSTR_INSTANCE.nextInt(4)) {
    // case 0 -> new PotionEffect(Potion.hunger.id, pollution / POISON_HUNGER_DIVISOR, 0);
    // case 1 -> new PotionEffect(
    // Potion.confusion.id,
    // Math.min(pollution / POISON_CONFUSION_DIVISOR, MAX_DURATION),
    // 1);
    // case 2 -> new PotionEffect(
    // Potion.poison.id,
    // Math.min(pollution / POISON_POISON_DIVISOR, MAX_DURATION),
    // pollution / POISON_HUNGER_DIVISOR);
    // case 3 -> new PotionEffect(
    // Potion.blindness.id,
    // Math.min(pollution / POISON_CONFUSION_DIVISOR, MAX_DURATION),
    // 1);
    // default -> null;
    // };
    //
    // entity.addPotionEffect(effect);
    // }
}
