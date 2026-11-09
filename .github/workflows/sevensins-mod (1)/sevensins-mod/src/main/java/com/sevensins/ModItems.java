package com.sevensins;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final Map<Sin, Item> SIGILS = new EnumMap<>(Sin.class);

    private ModItems() {}

    public static void register() {
        for (Sin sin : Sin.values()) {
            Item item = Registry.register(Registries.ITEM,
                    Identifier.of(SevenSinsMod.MOD_ID, sin.id + "_sigil"),
                    new SigilItem(sin, new Item.Settings().maxCount(1).fireproof()));
            SIGILS.put(sin, item);
        }
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS_AND_UTILITIES)
                .register(entries -> SIGILS.values().forEach(entries::add));
    }
}
