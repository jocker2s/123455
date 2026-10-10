package com.example.depression;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static final Item DIARY = Registry.register(
		Registries.ITEM,
		new Identifier(DepressionMod.MOD_ID, "diary"),
		new DiaryItem(new Item.Settings().maxCount(1)));

	private ModItems() {}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(DIARY));
	}
}
