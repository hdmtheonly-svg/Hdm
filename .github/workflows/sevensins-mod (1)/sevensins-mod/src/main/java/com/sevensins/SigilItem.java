package com.sevensins;

import net.minecraft.client.item.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class SigilItem extends Item {
    private final Sin sin;

    public SigilItem(Sin sin, Settings settings) {
        super(settings);
        this.sin = sin;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.success(stack);
        }

        if (player.isSneaking()) {
            // Sneak + use: embrace this sin (replaces any previous one)
            if (Sin.of(player) == sin) {
                Sin.unbind(player);
                player.sendMessage(Text.literal("You cast off " + sin.displayName + ".").formatted(sin.color), true);
            } else {
                Sin.bind(player, sin);
            }
            return TypedActionResult.success(stack);
        }

        if (Sin.of(player) != sin) {
            player.sendMessage(Text.literal("Sneak + use to embrace " + sin.displayName + " first.").formatted(Formatting.GRAY), true);
            return TypedActionResult.fail(stack);
        }

        if (sin.ability.use(player)) {
            player.getItemCooldownManager().set(this, sin.cooldownTicks);
            player.sendMessage(Text.literal(sin.abilityName + "!").formatted(sin.color, Formatting.BOLD), true);
            return TypedActionResult.success(stack);
        }
        return TypedActionResult.fail(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Ability: " + sin.abilityName + " (" + (sin.cooldownTicks / 20) + "s cooldown)").formatted(sin.color));
        tooltip.add(Text.literal("Sneak + Use: embrace / cast off this sin").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Use: activate ability").formatted(Formatting.GRAY));
    }
}
