package com.example.depression.client;

import com.example.depression.DepressionManager;
import com.example.depression.DepressionNetworking;
import com.example.depression.ModSounds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Клиентская часть: хранит значение шкалы и отвечает за звуки и навязчивые мысли. */
public class DepressionClient implements ClientModInitializer {
	public static final int THOUGHT_COUNT = 12;
	public static final int THOUGHT_TICKS = 110;

	/** Значение с сервера. */
	public static int value = 0;
	/** Плавно «догоняет» value — для отрисовки. */
	public static float shown = 0f;

	public static Text thought = null;
	public static int thoughtAge = 0;

	private static final Random RNG = new Random();
	private static final List<Delayed> DELAYED = new ArrayList<>();
	private static int thoughtCd = 0, whisperCd = 0, stepsCd = 0, heartCd = 0;
	private static int lastLevel = -1;

	private record Delayed(int[] ticks, Runnable task) {}

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(DepressionNetworking.SYNC,
			(client, handler, buf, responseSender) -> {
				int v = buf.readInt();
				client.execute(() -> value = v);
			});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
		ClientTickEvents.END_CLIENT_TICK.register(DepressionClient::tick);
		HudRenderCallback.EVENT.register(DepressionHud::render);
	}

	private static void reset() {
		value = 0;
		shown = 0f;
		thought = null;
		DELAYED.clear();
		lastLevel = -1;
	}

	private static int range(int lo, int hi) {
		return lo + RNG.nextInt(hi - lo + 1);
	}

	private static void later(int ticks, Runnable task) {
		DELAYED.add(new Delayed(new int[] {ticks}, task));
	}

	private static void tick(MinecraftClient mc) {
		if (mc.world == null || mc.player == null) {
			return;
		}
		if (mc.isPaused()) return;

		// Плавное движение шкалы.
		float diff = value - shown;
		shown = Math.abs(diff) < 0.5f ? value : shown + diff * 0.08f;

		// Отложенные действия (удары сердца, шаги).
		Iterator<Delayed> it = DELAYED.iterator();
		List<Runnable> due = new ArrayList<>();
		while (it.hasNext()) {
			Delayed d = it.next();
			if (--d.ticks()[0] <= 0) {
				due.add(d.task());
				it.remove();
			}
		}
		due.forEach(Runnable::run);

		if (thought != null && ++thoughtAge > THOUGHT_TICKS) thought = null;

		boolean active = mc.interactionManager != null && mc.interactionManager.hasStatusBars()
			&& mc.player.isAlive();
		int lvl = active ? DepressionManager.level(value) : 0;
		if (lvl != lastLevel) {
			// При смене уровня перепланируем события, чтобы они не копились.
			thoughtCd = interval(lvl, 2400, 4800, 1200, 2400, 500, 1200);
			whisperCd = interval(lvl, 0, 0, 2400, 4800, 1000, 2200);
			stepsCd = interval(lvl, 0, 0, 3600, 7200, 1800, 3600);
			heartCd = interval(lvl, 0, 0, 2400, 4800, 1200, 2400);
			lastLevel = lvl;
		}
		if (lvl == 0) return;

		if (--thoughtCd <= 0) {
			showThought();
			thoughtCd = interval(lvl, 2400, 4800, 1200, 2400, 500, 1200);
		}
		if (lvl >= 2) {
			if (--whisperCd <= 0) {
				whisper(mc, lvl);
				whisperCd = interval(lvl, 0, 0, 2400, 4800, 1000, 2200);
			}
			if (--stepsCd <= 0) {
				footsteps(mc, lvl);
				stepsCd = interval(lvl, 0, 0, 3600, 7200, 1800, 3600);
			}
			if (--heartCd <= 0) {
				heartbeat(mc, lvl);
				heartCd = interval(lvl, 0, 0, 2400, 4800, 1200, 2400);
			}
		}
	}

	private static int interval(int lvl, int a1, int b1, int a2, int b2, int a3, int b3) {
		return switch (lvl) {
			case 1 -> a1 > 0 ? range(a1, b1) : 1_000_000;
			case 2 -> a2 > 0 ? range(a2, b2) : 1_000_000;
			case 3 -> a3 > 0 ? range(a3, b3) : 1_000_000;
			default -> 1_000_000;
		};
	}

	private static void showThought() {
		thought = Text.translatable("thought.depression." + (1 + RNG.nextInt(THOUGHT_COUNT)));
		thoughtAge = 0;
	}

	private static void play(MinecraftClient mc, SoundEvent sound, Vec3d at, float volume, float pitch) {
		ClientWorld world = mc.world;
		if (world == null) return;
		world.playSound(at.x, at.y, at.z, sound, SoundCategory.AMBIENT, volume, pitch, false);
	}

	/** Серия приглушённых ударов сердца. */
	private static void heartbeat(MinecraftClient mc, int lvl) {
		int beats = lvl == 3 ? 8 : 5;
		int gap = lvl == 3 ? 18 : 24;
		float volume = lvl == 3 ? 0.9f : 0.6f;
		for (int i = 0; i < beats; i++) {
			later(1 + i * gap, () -> {
				if (mc.player != null) {
					play(mc, ModSounds.HEARTBEAT, mc.player.getEyePos(), volume, 1.0f);
				}
			});
		}
	}

	/** Далёкий шёпот где-то рядом (чаще — сзади). */
	private static void whisper(MinecraftClient mc, int lvl) {
		if (mc.player == null) return;
		double angle = Math.toRadians(mc.player.getYaw() + 180 + RNG.nextInt(120) - 60);
		Vec3d eye = mc.player.getEyePos();
		Vec3d at = eye.add(-Math.sin(angle) * 3.5, 0, Math.cos(angle) * 3.5);
		play(mc, ModSounds.WHISPER, at, lvl == 3 ? 0.9f : 0.6f, 0.9f + RNG.nextFloat() * 0.15f);
	}

	/** Тихие шаги за спиной — как будто кто-то подходит. */
	private static void footsteps(MinecraftClient mc, int lvl) {
		int steps = lvl == 3 ? 7 : 5;
		for (int i = 0; i < steps; i++) {
			final int n = i;
			later(1 + i * 11, () -> {
				if (mc.player == null || mc.world == null) return;
				BlockPos under = mc.player.getBlockPos().down();
				SoundEvent step = mc.world.getBlockState(under).getSoundGroup().getStepSound();
				Vec3d look = Vec3d.fromPolar(0, mc.player.getYaw());
				double dist = 6.0 - n * 0.6;
				Vec3d at = mc.player.getPos().add(look.multiply(-dist)).add(0, 0.1, 0);
				play(mc, step, at, 0.5f + n * 0.05f, 0.85f + RNG.nextFloat() * 0.1f);
			});
		}
	}
}
