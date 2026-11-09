package com.sevensins;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class Abilities {
    private Abilities() {}

    private static List<LivingEntity> nearby(ServerPlayerEntity p, double r) {
        return p.getWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(r),
                e -> e != p && e.isAlive() && e.squaredDistanceTo(p) <= r * r);
    }

    private static void fx(ServerPlayerEntity p, ParticleEffect particle, int count, double spread, SoundEvent sound) {
        ServerWorld w = p.getServerWorld();
        w.spawnParticles(particle, p.getX(), p.getBodyY(0.5), p.getZ(), count, spread, 0.5, spread, 0.05);
        w.playSound(null, p.getX(), p.getY(), p.getZ(), sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void effect(LivingEntity e, net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> eff, int seconds, int amp) {
        e.addStatusEffect(new StatusEffectInstance(eff, seconds * 20, amp));
    }

    // ---------------------------------------------------------------- WRATH
    /** Explosive slam: damages, ignites and launches everything within 6 blocks. Costs you health. */
    public static boolean wrath(ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        for (LivingEntity e : nearby(p, 6)) {
            e.damage(w.getDamageSources().playerAttack(p), 8.0f);
            e.setOnFireFor(5);
            e.takeKnockback(1.6, p.getX() - e.getX(), p.getZ() - e.getZ());
        }
        p.setHealth(Math.max(1.0f, p.getHealth() - 4.0f)); // rage burns you
        effect(p, StatusEffects.STRENGTH, 10, 1);
        fx(p, ParticleTypes.EXPLOSION, 6, 2.5, SoundEvents.ENTITY_GENERIC_EXPLODE.value());
        fx(p, ParticleTypes.FLAME, 80, 3.0, SoundEvents.ENTITY_BLAZE_SHOOT);
        return true;
    }

    // ---------------------------------------------------------------- GREED
    /** 20s of fortune and speed-mining, paid for with a ravenous hunger. */
    public static boolean greed(ServerPlayerEntity p) {
        effect(p, StatusEffects.HASTE, 20, 2);
        effect(p, StatusEffects.LUCK, 20, 2);
        effect(p, StatusEffects.SPEED, 20, 1);
        effect(p, StatusEffects.HUNGER, 20, 2);
        fx(p, ParticleTypes.HAPPY_VILLAGER, 40, 1.0, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
        fx(p, ParticleTypes.WAX_ON, 30, 1.0, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME);
        return true;
    }

    // ---------------------------------------------------------------- SLOTH
    /** A field of crushing lethargy around you; you recover while everyone else crawls. */
    public static boolean sloth(ServerPlayerEntity p) {
        for (LivingEntity e : nearby(p, 10)) {
            effect(e, StatusEffects.SLOWNESS, 10, 2);
            effect(e, StatusEffects.WEAKNESS, 10, 1);
            effect(e, StatusEffects.MINING_FATIGUE, 10, 1);
        }
        effect(p, StatusEffects.REGENERATION, 10, 1);
        fx(p, ParticleTypes.SQUID_INK, 60, 4.0, SoundEvents.ENTITY_WARDEN_HEARTBEAT);
        return true;
    }

    // ---------------------------------------------------------------- ENVY
    /** Looks at a target within 20 blocks and steals its beneficial effects. */
    public static boolean envy(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1.0f);
        Vec3d eye = p.getEyePos();
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : nearby(p, 20)) {
            Vec3d to = e.getEyePos().subtract(eye);
            double d = to.length();
            if (d < 0.001) continue;
            if (look.dotProduct(to.normalize()) > 0.96 && d < bestDist && p.canSee(e)) {
                best = e;
                bestDist = d;
            }
        }
        if (best == null) {
            p.sendMessage(Text.literal("There is no one to covet."), true);
            return false;
        }
        int stolen = 0;
        for (StatusEffectInstance inst : new ArrayList<>(best.getStatusEffects())) {
            if (inst.getEffectType().value().isBeneficial()) {
                p.addStatusEffect(new StatusEffectInstance(inst));
                best.removeStatusEffect(inst.getEffectType());
                stolen++;
            }
        }
        effect(best, StatusEffects.WEAKNESS, 10, 1);
        effect(best, StatusEffects.GLOWING, 10, 0);
        if (stolen == 0) effect(p, StatusEffects.SPEED, 8, 2); // nothing to steal: pure spite-speed
        fx(p, ParticleTypes.HAPPY_VILLAGER, 30, 1.0, SoundEvents.ENTITY_EVOKER_CAST_SPELL);
        return true;
    }

    // ---------------------------------------------------------------- GLUTTONY
    /** Instantly devours a food item from your inventory for a huge heal + absorption. */
    public static boolean gluttony(ServerPlayerEntity p) {
        ItemStack chosen = ItemStack.EMPTY;
        int bestNutrition = -1;
        for (ItemStack s : p.getInventory().main) {
            FoodComponent food = s.get(DataComponentTypes.FOOD);
            if (food != null && food.nutrition() > bestNutrition) {
                bestNutrition = food.nutrition();
                chosen = s;
            }
        }
        if (chosen.isEmpty()) {
            p.sendMessage(Text.literal("You have nothing to devour."), true);
            return false;
        }
        FoodComponent food = chosen.get(DataComponentTypes.FOOD);
        p.getHungerManager().add(food.nutrition(), food.saturation());
        p.heal(food.nutrition() * 1.5f);
        chosen.decrement(1);
        effect(p, StatusEffects.ABSORPTION, 30, 2);
        effect(p, StatusEffects.REGENERATION, 5, 2);
        fx(p, ParticleTypes.HEART, 10, 0.6, SoundEvents.ENTITY_GENERIC_EAT);
        return true;
    }

    // ---------------------------------------------------------------- LUST
    /** Entrances nearby mobs: drags them to you, glows them and strips their strength. */
    public static boolean lust(ServerPlayerEntity p) {
        for (LivingEntity e : nearby(p, 12)) {
            if (e instanceof ServerPlayerEntity) {
                effect(e, StatusEffects.SLOWNESS, 5, 1);
                continue;
            }
            Vec3d pull = p.getPos().subtract(e.getPos()).normalize().multiply(1.1);
            e.setVelocity(pull.x, 0.3, pull.z);
            e.velocityModified = true;
            if (e instanceof MobEntity m) m.setTarget(null);
            effect(e, StatusEffects.WEAKNESS, 8, 4);
            effect(e, StatusEffects.GLOWING, 8, 0);
        }
        effect(p, StatusEffects.REGENERATION, 8, 2);
        fx(p, ParticleTypes.HEART, 40, 4.0, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM);
        return true;
    }

    // ---------------------------------------------------------------- PRIDE
    /** Six near-invulnerable seconds and a shockwave. Afterwards, humility: you are weakened. */
    public static boolean pride(ServerPlayerEntity p) {
        for (LivingEntity e : nearby(p, 5)) {
            e.takeKnockback(2.0, p.getX() - e.getX(), p.getZ() - e.getZ());
        }
        effect(p, StatusEffects.RESISTANCE, 6, 4);
        effect(p, StatusEffects.ABSORPTION, 15, 2);
        effect(p, StatusEffects.STRENGTH, 6, 1);
        effect(p, StatusEffects.WEAKNESS, 16, 1); // humility tax (overrides strength after a moment of glory)
        fx(p, ParticleTypes.END_ROD, 80, 2.0, SoundEvents.BLOCK_BEACON_ACTIVATE);
        return true;
    }
}
