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
    private static final int MAX_DURATION = 1000;
    private static final int MAX_ATTEMPTS = 20;
    private static final int CHUNK_HEIGHT = 256;
    private static final int CHUNK_SIZE = 16;

    public void applyPollutionEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        if (pollution > GTMod.proxy.mPollutionSmogLimit) {
            applySmogEffects(world, chunkPos, pollution);
        }
    }

    private void applySmogEffects(World world, ChunkCoordIntPair chunkPos, int pollution) {
        AxisAlignedBB chunkBounds = createChunkBoundingBox(chunkPos);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, chunkBounds);

        entities.stream()
            .filter(this::shouldApplyEffects)
            .forEach(entity -> processEntityEffects(entity, pollution));

        if (pollution > GTMod.proxy.mPollutionVegetationLimit) {
            damageVegetation(world, chunkPos, pollution);
        }
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
        if (!HazardProtection.isWearingFullGasHazmat(entity)) {
            applyNegativeEffects(entity, pollution);
        }

        if (pollution > GTMod.proxy.mPollutionPoisonLimit) {
            applyPoisonEffects(entity, pollution);
        }
    }

    private void applyNegativeEffects(EntityLivingBase entity, int pollution) {
        int duration = Math.min(pollution / SMOG_DURATION_DIVISOR, MAX_DURATION);
        int amplifier = pollution / SMOG_AMPLIFIER_DIVISOR;

        PotionEffect effect = switch (XSTR_INSTANCE.nextInt(3)) {
            case 0 -> new PotionEffect(Potion.digSlowdown.id, duration, amplifier);
            case 1 -> new PotionEffect(Potion.weakness.id, duration, amplifier);
            case 2 -> new PotionEffect(Potion.moveSlowdown.id, duration, amplifier);
            default -> null;
        };

        entity.addPotionEffect(effect);
    }

    private void applyPoisonEffects(EntityLivingBase entity, int pollution) {
        PotionEffect effect = switch (XSTR_INSTANCE.nextInt(4)) {
            case 0 -> new PotionEffect(Potion.hunger.id, pollution / POISON_HUNGER_DIVISOR, 0);
            case 1 -> new PotionEffect(
                Potion.confusion.id,
                Math.min(pollution / POISON_CONFUSION_DIVISOR, MAX_DURATION),
                1);
            case 2 -> new PotionEffect(
                Potion.poison.id,
                Math.min(pollution / POISON_POISON_DIVISOR, MAX_DURATION),
                pollution / POISON_HUNGER_DIVISOR);
            case 3 -> new PotionEffect(
                Potion.blindness.id,
                Math.min(pollution / POISON_CONFUSION_DIVISOR, MAX_DURATION),
                1);
            default -> null;
        };

        entity.addPotionEffect(effect);
    }

    private void damageVegetation(World world, ChunkCoordIntPair chunkPos, int pollution) {
        int attempts = Math.min(MAX_ATTEMPTS, pollution / VEGETATION_ATTEMPTS_DIVISOR);
        boolean sourRain = pollution > GTMod.proxy.mPollutionSourRainLimit;

        for (int i = 0; i < attempts; i++) {
            int baseX = chunkPos.chunkXPos << 4;
            int baseZ = chunkPos.chunkZPos << 4;
            int x = baseX + XSTR_INSTANCE.nextInt(CHUNK_SIZE);
            int y = 60 + (-i + XSTR_INSTANCE.nextInt(i * 2 + 1));
            int z = baseZ + XSTR_INSTANCE.nextInt(CHUNK_SIZE);
            PollutionBlockDamager.damageBlock(world, x, y, z, sourRain);
        }
    }
}
