package gregtech.common.pollutionRework.handlers;

import static gregtech.api.objects.XSTR.XSTR_INSTANCE;

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
import gregtech.common.pollutionRework.PollutionBlockDamager;

public class PollutionEffectHandler {

    private static final int SMOG_DURATION_DIVISOR = 1000;
    private static final int SMOG_AMPLIFIER_DIVISOR = 400000;
    private static final int POISON_HUNGER_DIVISOR = 500000;
    private static final int POISON_CONFUSION_DIVISOR = 2000;
    private static final int POISON_POISON_DIVISOR = 4000;
    private static final int VEGETATION_ATTEMPTS_DIVISOR = 25000;

    public void applyPollutionEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        if (pollution > GTMod.proxy.mPollutionSmogLimit) {
            applySmogEffects(world, chunkPos, pollution);
        }
    }

    private void applySmogEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        AxisAlignedBB chunkBounds = createChunkBoundingBox(chunkPos);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, chunkBounds);

        for (EntityLivingBase entity : entities) {
            if (shouldSkipEntity(entity)) continue;

            if (!HazardProtection.isWearingFullGasHazmat(entity)) {
                applyNegativeEffects(entity, pollution);
            }

            if (pollution > GTMod.proxy.mPollutionPoisonLimit) {
                applyPoisonEffects(entity, pollution);
            }
        }

        if (pollution > GTMod.proxy.mPollutionVegetationLimit) {
            damageVegetation(world, chunkPos, pollution);
        }
    }

    private AxisAlignedBB createChunkBoundingBox(ChunkCoordIntPair chunkPos) {
        return AxisAlignedBB.getBoundingBox(
            chunkPos.chunkXPos << 4,
            0,
            chunkPos.chunkZPos << 4,
            (chunkPos.chunkXPos << 4) + 16,
            256,
            (chunkPos.chunkZPos << 4) + 16);
    }

    private boolean shouldSkipEntity(EntityLivingBase entity) {
        return entity instanceof EntityPlayerMP && ((EntityPlayerMP) entity).capabilities.isCreativeMode;
    }

    private void applyNegativeEffects(EntityLivingBase entity, int pollution) {
        int duration = Math.min(pollution / SMOG_DURATION_DIVISOR, 1000);
        int amplifier = pollution / SMOG_AMPLIFIER_DIVISOR;

        switch (XSTR_INSTANCE.nextInt(3)) {
            case 0:
                entity.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, duration, amplifier));
                break;
            case 1:
                entity.addPotionEffect(new PotionEffect(Potion.weakness.id, duration, amplifier));
                break;
            case 2:
                entity.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, duration, amplifier));
                break;
        }
    }

    private void applyPoisonEffects(EntityLivingBase entity, int pollution) {
        switch (XSTR_INSTANCE.nextInt(4)) {
            case 0:
                entity.addPotionEffect(new PotionEffect(Potion.hunger.id, pollution / POISON_HUNGER_DIVISOR, 0));
                break;
            case 1:
                entity.addPotionEffect(
                    new PotionEffect(Potion.confusion.id, Math.min(pollution / POISON_CONFUSION_DIVISOR, 1000), 1));
                break;
            case 2:
                entity.addPotionEffect(
                    new PotionEffect(
                        Potion.poison.id,
                        Math.min(pollution / POISON_POISON_DIVISOR, 1000),
                        pollution / POISON_HUNGER_DIVISOR));
                break;
            case 3:
                entity.addPotionEffect(
                    new PotionEffect(Potion.blindness.id, Math.min(pollution / POISON_CONFUSION_DIVISOR, 1000), 1));
                break;
        }
    }

    private void damageVegetation(World world, ChunkCoordIntPair chunkPos, int pollution) {
        int attempts = Math.min(20, pollution / VEGETATION_ATTEMPTS_DIVISOR);
        boolean sourRain = pollution > GTMod.proxy.mPollutionSourRainLimit;

        for (int i = 0; i < attempts; i++) {
            int x = (chunkPos.chunkXPos << 4) + XSTR_INSTANCE.nextInt(16);
            int y = 60 + (-i + XSTR_INSTANCE.nextInt(i * 2 + 1));
            int z = (chunkPos.chunkZPos << 4) + XSTR_INSTANCE.nextInt(16);
            PollutionBlockDamager.damageBlock(world, x, y, z, sourRain);
        }
    }
}
