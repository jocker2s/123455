package com.example.depression;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
	public static final SoundEvent HEARTBEAT = register("heartbeat");
	public static final SoundEvent WHISPER = register("whisper");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		Identifier id = new Identifier(DepressionMod.MOD_ID, name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void init() {}
}
