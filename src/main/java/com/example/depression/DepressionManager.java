package com.example.depression;

import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DepressionManager {
	public static final int MAX = 2400;
	public static final int LEVEL_1 = 600;
	public static final int LEVEL_2 = 1200;
	public static final int LEVEL_3 = 1800;

	/** «Радость»: шкала ≤ JOY_THRESHOLD держится JOY_AFTER_SECONDS секунд подряд. */
	public static final int JOY_THRESHOLD = 150;
	public static final int JOY_AFTER_SECONDS = 300;

	private static final int SOCIAL_RADIUS = 16;
	private static final int PET_RADIUS = 8;
	private static final int SLEEPLESS_TICKS = 72000; // 3 игровых суток без сна

	// Уют (облегчение в секунду)
	private static final int CAMPFIRE_RADIUS = 5;
	private static final int MUSIC_RADIUS = 8;
	// Одноразовое облегчение
	private static final int RELIEF_FISH = 60;
	private static final int RELIEF_CAKE = 150;
	private static final int RELIEF_FLOWER = 60;
	private static final int FLOWER_COOLDOWN = 1200;
	// Сон
	private static final int SLEEP_RELIEF_COMPANY = 900;
	private static final int SLEEP_RELIEF_ALONE = 450;

	private static final Map<UUID, int[]> LAST_STATS = new HashMap<>();
	private static final Map<UUID, Map<String, Integer>> COOLDOWNS = new HashMap<>();

	private DepressionManager() {}

	public static DepressionState state(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager()
			.getOrCreate(DepressionState::fromNbt, DepressionState::new, "depression");
	}

	public static int level(int value) {
		if (value >= LEVEL_3) return 3;
		if (value >= LEVEL_2) return 2;
		if (value >= LEVEL_1) return 1;
		return 0;
	}

	/** Уровень по активному эффекту (работает и на клиенте, и на сервере). 0 — эффекта нет. */
	public static int effectLevel(LivingEntity entity) {
		StatusEffectInstance inst = entity.getStatusEffect(ModEffects.DEPRESSION);
		return inst == null ? 0 : inst.getAmplifier() + 1;
	}

	public static int get(ServerPlayerEntity player) {
		return state(player.getServer()).get(player.getUuid());
	}

	/** Вызывается каждый тик сервера, работает раз в секунду. */
	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 20 != 0) return;

		DepressionState state = state(server);
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isCreative() || player.isSpectator() || player.isSleeping()) continue;

			UUID id = player.getUuid();
			int old = state.get(id);
			int change = delta(player) - activityRelief(player, server.getTicks());
			int now = clamp(old + change);
			state.set(id, now);

			updateJoy(player, state, now);
			apply(player, old, now);
			if (now != old) DepressionNetworking.send(player, now);
		}
	}

	/** Что происходит с настроением за одну секунду. Плюс — хуже, минус — лучше. */
	private static int delta(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos pos = player.getBlockPos();
		int change = 0;

		// Что давит.
		if (world.getLightLevel(pos) <= 4) change += 1;
		if (player.getStatHandler().getStat(
			Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST)) > SLEEPLESS_TICKS) change += 1;
		if (player.getHealth() <= 6.0f) change += 1;

		// Что поддерживает.
		boolean players = !world.getPlayers(p -> p != player && !p.isSpectator()
			&& p.squaredDistanceTo(player) < SOCIAL_RADIUS * SOCIAL_RADIUS).isEmpty();
		boolean pet = !world.getEntitiesByClass(TameableEntity.class,
			player.getBoundingBox().expand(PET_RADIUS), t -> t.isOwner(player)).isEmpty();
		boolean villager = !world.getEntitiesByClass(VillagerEntity.class,
			player.getBoundingBox().expand(PET_RADIUS), v -> true).isEmpty();
		boolean sunny = world.getDimension().hasSkyLight() && world.isDay()
			&& !world.isRaining() && world.isSkyVisible(pos);

		if (!players && !pet && !villager) change += 1; // одиночество
		if (players) change -= 2;
		if (pet) change -= 2;
		if (villager) change -= 1;
		if (sunny) change -= 2;

		// Уют: костёр и музыка.
		boolean fire = false;
		boolean music = false;
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int dx = -MUSIC_RADIUS; dx <= MUSIC_RADIUS && !(fire && music); dx++) {
			for (int dz = -MUSIC_RADIUS; dz <= MUSIC_RADIUS && !(fire && music); dz++) {
				for (int dy = -3; dy <= 3; dy++) {
					m.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
					BlockState s = world.getBlockState(m);
					if (s.isAir()) continue;
					if (!fire && Math.abs(dx) <= CAMPFIRE_RADIUS && Math.abs(dz) <= CAMPFIRE_RADIUS
						&& s.getBlock() instanceof CampfireBlock && s.get(CampfireBlock.LIT)) {
						fire = true;
					} else if (!music && s.isOf(net.minecraft.block.Blocks.JUKEBOX)) {
						BlockEntity be = world.getBlockEntity(m);
						if (be instanceof JukeboxBlockEntity jukebox && jukebox.isPlayingRecord()) {
							music = true;
						}
					}
				}
			}
		}
		if (fire) change -= 2;
		if (music) change -= 2;

		return change;
	}

	/** Одноразовое облегчение от рыбалки, торта и посаженных цветов (считаем по статистике игрока). */
	private static int activityRelief(ServerPlayerEntity player, int nowTick) {
		int fish = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.FISH_CAUGHT));
		int cake = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.EAT_CAKE_SLICE));
		int flowers = 0;
		for (RegistryEntry<Item> entry : Registries.ITEM.iterateEntries(ItemTags.FLOWERS)) {
			Stat<Item> stat = Stats.USED.getOrCreateStat(entry.value());
			flowers += player.getStatHandler().getStat(stat);
		}

		UUID id = player.getUuid();
		int[] last = LAST_STATS.get(id);
		LAST_STATS.put(id, new int[] {fish, cake, flowers});
		if (last == null) return 0; // первый замер — просто запоминаем

		int relief = 0;
		if (fish > last[0]) relief += RELIEF_FISH * (fish - last[0]);
		if (cake > last[1]) relief += RELIEF_CAKE * (cake - last[1]);
		if (flowers > last[2] && tryCooldown(id, "flower", nowTick, FLOWER_COOLDOWN)) relief += RELIEF_FLOWER;
		return relief;
	}

	/** true, если действие разрешено (и тут же запускает перезарядку). */
	public static boolean tryCooldown(UUID id, String key, int nowTick, int cooldownTicks) {
		Map<String, Integer> map = COOLDOWNS.computeIfAbsent(id, k -> new HashMap<>());
		Integer last = map.get(key);
		if (last != null && nowTick >= last && nowTick - last < cooldownTicks) return false;
		map.put(key, nowTick);
		return true;
	}

	private static void updateJoy(ServerPlayerEntity player, DepressionState state, int now) {
		UUID id = player.getUuid();
		int calm = now <= JOY_THRESHOLD ? Math.min(state.getCalm(id) + 1, JOY_AFTER_SECONDS) : 0;
		state.setCalm(id, calm);
		if (calm >= JOY_AFTER_SECONDS) {
			player.addStatusEffect(new StatusEffectInstance(ModEffects.JOY, 200, 0, false, false, true));
		}
	}

	private static void apply(ServerPlayerEntity player, int oldValue, int newValue) {
		int lvl = level(newValue);
		if (lvl > 0) {
			player.addStatusEffect(new StatusEffectInstance(
				ModEffects.DEPRESSION, 100, lvl - 1, false, false, true));
		} else if (player.hasStatusEffect(ModEffects.DEPRESSION)) {
			player.removeStatusEffect(ModEffects.DEPRESSION);
		}

		int oldLvl = level(oldValue);
		if (lvl > oldLvl) {
			player.sendMessage(Text.translatable("message.depression.worse." + lvl), true);
		} else if (lvl < oldLvl) {
			player.sendMessage(Text.translatable("message.depression.better." + lvl), true);
		}
	}

	/** Изменить шкалу (минус — облегчение), применить эффект и отправить клиенту. */
	public static void adjust(ServerPlayerEntity player, int delta) {
		set(player, get(player) + delta);
	}

	public static void set(ServerPlayerEntity player, int value) {
		DepressionState state = state(player.getServer());
		int old = state.get(player.getUuid());
		int now = clamp(value);
		state.set(player.getUuid(), now);
		if (now > JOY_THRESHOLD) state.setCalm(player.getUuid(), 0);
		apply(player, old, now);
		DepressionNetworking.send(player, now);
	}

	/** Есть ли «свои» рядом: другой игрок или приручённый питомец. */
	public static boolean hasCompany(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		boolean players = !world.getPlayers(p -> p != player && !p.isSpectator()
			&& p.squaredDistanceTo(player) < SOCIAL_RADIUS * SOCIAL_RADIUS).isEmpty();
		boolean pet = !world.getEntitiesByClass(TameableEntity.class,
			player.getBoundingBox().expand(PET_RADIUS), t -> t.isOwner(player)).isEmpty();
		return players || pet;
	}

	public static void onWokeUp(ServerPlayerEntity player) {
		// Награда только за настоящий сон до утра, а не за прыжки в кровать и обратно.
		if (!player.getServerWorld().isDay()) return;
		adjust(player, -(hasCompany(player) ? SLEEP_RELIEF_COMPANY : SLEEP_RELIEF_ALONE));
	}

	public static void reset(ServerPlayerEntity player) {
		DepressionState state = state(player.getServer());
		state.set(player.getUuid(), 0);
		state.setCalm(player.getUuid(), 0);
		player.removeStatusEffect(ModEffects.DEPRESSION);
		DepressionNetworking.send(player, 0);
	}

	public static int clamp(int value) {
		return Math.max(0, Math.min(MAX, value));
	}
}
