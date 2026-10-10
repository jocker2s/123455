package com.example.depression;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEffects {
	public static final StatusEffect DEPRESSION = Registry.register(
		Registries.STATUS_EFFECT,
		new Identifier(DepressionMod.MOD_ID, "depression"),
		new DepressionEffect());

	public static final StatusEffect JOY = Registry.register(
		Registries.STATUS_EFFECT,
		new Identifier(DepressionMod.MOD_ID, "joy"),
		new JoyEffect());

	private ModEffects() {}

	/** Вызов нужен, чтобы сработала статическая инициализация. */
	public static void init() {}
}
