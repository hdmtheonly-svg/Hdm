package com.sevensins;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.command.argument.EntityArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;

public class SevenSinsMod implements ModInitializer {
    public static final String MOD_ID = "sevensins";

    @Override
    public void onInitialize() {
        ModItems.register();

        // Passive buffs / debuffs, refreshed every second
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int ticks = server.getTicks();
            if (ticks % 20 != 0) return;
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                Sin sin = Sin.of(p);
                if (sin == null || p.isSpectator()) continue;
                sin.applyPassives(p);
                // Lust: "blinded by desire" - periodic bursts of darkness
                if (sin == Sin.LUST && ticks % 400 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 100, 0, true, false, true));
                }
            }
        });

        // /sin <name|none> [player]  (op only, handy for testing)
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            LiteralArgumentBuilder<ServerCommandSource> root =
                    CommandManager.literal("sin").requires(src -> src.hasPermissionLevel(2));
            for (Sin sin : Sin.values()) {
                root.then(CommandManager.literal(sin.id)
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                            Sin.bind(p, sin);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = EntityArgumentType.getPlayer(ctx, "target");
                                    Sin.bind(p, sin);
                                    return Command.SINGLE_SUCCESS;
                                })));
            }
            root.then(CommandManager.literal("none").executes(ctx -> {
                ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                Sin.unbind(p);
                p.sendMessage(Text.literal("You have cast off your sin."), true);
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(root);
        });
    }
}
