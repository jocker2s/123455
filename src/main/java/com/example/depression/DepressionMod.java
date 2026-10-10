package com.example.depression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DepressionMod implements ModInitializer {
	public static final String MOD_ID = "depression";

	@Override
	public void onInitialize() {
		ModEffects.init();
		ModItems.init();
		ModSounds.init();

		ServerTickEvents.END_SERVER_TICK.register(DepressionManager::tick);

		// Нормальный сон сильно помогает (а в одиночестве — слабее).
		EntitySleepEvents.STOP_SLEEPING.register((entity, pos) -> {
			if (entity instanceof ServerPlayerEntity player) {
				DepressionManager.onWokeUp(player);
			}
		});

		// Бессонница: на тяжёлой стадии ночью уснуть получается не всегда.
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (player instanceof ServerPlayerEntity sp
				&& DepressionManager.level(DepressionManager.get(sp)) >= 3
				&& sp.getServerWorld().isNight()
				&& sp.getRandom().nextFloat() < 0.5f) {
				sp.sendMessage(Text.translatable("message.depression.insomnia"), true);
				return PlayerEntity.SleepFailureReason.NOT_POSSIBLE_HERE;
			}
			return null;
		});

		// После смерти начинаем с чистого листа.
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
			DepressionManager.reset(newPlayer));

		// Клиенту сразу сообщаем текущее значение.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			DepressionNetworking.send(handler.getPlayer(), DepressionManager.get(handler.getPlayer())));

		// Забота: питомцы и цветы в подарок.
		UseEntityCallback.EVENT.register(DepressionInteractions::onUseEntity);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			DepressionCommand.register(dispatcher));
	}
}
