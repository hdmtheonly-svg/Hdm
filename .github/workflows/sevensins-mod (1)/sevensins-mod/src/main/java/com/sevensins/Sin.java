package com.sevensins;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum Sin {
    WRATH("wrath", "Wrath", Formatting.RED, "Inferno Slam", 400,
            new Fx[]{ new Fx(StatusEffects.STRENGTH, 0) },
            new Fx[]{ new Fx(StatusEffects.HUNGER, 0) },
            Abilities::wrath),

    GREED("greed", "Greed", Formatting.GOLD, "Gold Fever", 600,
            new Fx[]{ new Fx(StatusEffects.HASTE, 0), new Fx(StatusEffects.LUCK, 0) },
            new Fx[]{ new Fx(StatusEffects.SLOWNESS, 0) },
            Abilities::greed),

    SLOTH("sloth", "Sloth", Formatting.DARK_AQUA, "Lethargy Field", 500,
            new Fx[]{ new Fx(StatusEffects.RESISTANCE, 0), new Fx(StatusEffects.REGENERATION, 0) },
            new Fx[]{ new Fx(StatusEffects.MINING_FATIGUE, 1) },
            Abilities::sloth),

    ENVY("envy", "Envy", Formatting.GREEN, "Covet", 400,
            new Fx[]{ new Fx(StatusEffects.SPEED, 0), new Fx(StatusEffects.NIGHT_VISION, 0) },
            new Fx[]{ new Fx(StatusEffects.WEAKNESS, 0) },
            Abilities::envy),

    GLUTTONY("gluttony", "Gluttony", Formatting.YELLOW, "Devour", 300,
            new Fx[]{ new Fx(StatusEffects.ABSORPTION, 0), new Fx(StatusEffects.REGENERATION, 0) },
            new Fx[]{ new Fx(StatusEffects.HUNGER, 1) },
            Abilities::gluttony),

    LUST("lust", "Lust", Formatting.LIGHT_PURPLE, "Allure", 500,
            new Fx[]{ new Fx(StatusEffects.REGENERATION, 1), new Fx(StatusEffects.JUMP_BOOST, 0) },
            new Fx[]{ /* periodic Darkness, handled in SevenSinsMod */ },
            Abilities::lust),

    PRIDE("pride", "Pride", Formatting.DARK_PURPLE, "Crown of Pride", 900,
            new Fx[]{ new Fx(StatusEffects.HEALTH_BOOST, 1), new Fx(StatusEffects.RESISTANCE, 0) },
            new Fx[]{ new Fx(StatusEffects.WEAKNESS, 0) },
            Abilities::pride);

    public record Fx(RegistryEntry<StatusEffect> effect, int amp) {}

    @FunctionalInterface
    public interface Ability { boolean use(ServerPlayerEntity player); }

    public final String id;
    public final String displayName;
    public final Formatting color;
    public final String abilityName;
    public final int cooldownTicks;
    public final Fx[] buffs;
    public final Fx[] debuffs;
    public final Ability ability;

    Sin(String id, String displayName, Formatting color, String abilityName, int cooldownTicks,
        Fx[] buffs, Fx[] debuffs, Ability ability) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.abilityName = abilityName;
        this.cooldownTicks = cooldownTicks;
        this.buffs = buffs;
        this.debuffs = debuffs;
        this.ability = ability;
    }

    public String tag() { return "sin_" + id; }

    public void applyPassives(ServerPlayerEntity p) {
        for (Fx f : buffs)   p.addStatusEffect(new StatusEffectInstance(f.effect, 260, f.amp, true, false, true));
        for (Fx f : debuffs) p.addStatusEffect(new StatusEffectInstance(f.effect, 260, f.amp, true, false, true));
    }

    public void clearPassives(ServerPlayerEntity p) {
        for (Fx f : buffs)   p.removeStatusEffect(f.effect);
        for (Fx f : debuffs) p.removeStatusEffect(f.effect);
    }

    /** Sin is stored as a command tag on the player, so it persists through death and relogs. */
    public static Sin of(ServerPlayerEntity p) {
        for (Sin s : values()) if (p.getCommandTags().contains(s.tag())) return s;
        return null;
    }

    public static void unbind(ServerPlayerEntity p) {
        Sin old = of(p);
        if (old != null) old.clearPassives(p);
        for (Sin s : values()) p.removeCommandTag(s.tag());
    }

    public static void bind(ServerPlayerEntity p, Sin sin) {
        unbind(p);
        p.addCommandTag(sin.tag());
        sin.applyPassives(p);
        p.sendMessage(Text.literal("You embrace " + sin.displayName + ".").formatted(sin.color), true);
    }
}
