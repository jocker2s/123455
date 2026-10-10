package com.example.depression;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/** Сервер → клиент: текущее значение шкалы депрессии. */
public final class DepressionNetworking {
	public static final Identifier SYNC = new Identifier(DepressionMod.MOD_ID, "sync");

	private DepressionNetworking() {}

	public static void send(ServerPlayerEntity player, int value) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(value);
		ServerPlayNetworking.send(player, SYNC, buf);
	}
}
