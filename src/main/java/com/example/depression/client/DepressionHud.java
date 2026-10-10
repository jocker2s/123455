package com.example.depression.client;

import com.example.depression.DepressionManager;
import com.example.depression.ModEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Шкала депрессии над полоской голода, виньетка «туннельного зрения» и навязчивые мысли. */
public final class DepressionHud {
	private static final Identifier MOOD = new Identifier("depression", "textures/gui/mood.png");
	private static final Identifier VIGNETTE = new Identifier("depression", "textures/gui/vignette.png");

	private static final int BAR_WIDTH = 71;

	private DepressionHud() {}

	public static void render(DrawContext ctx, float tickDelta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.interactionManager == null || !mc.interactionManager.hasStatusBars()) {
			return;
		}
		int w = ctx.getScaledWindowWidth();
		int h = ctx.getScaledWindowHeight();
		float time = (mc.player.age + tickDelta);

		drawAtmosphere(ctx, w, h, time);

		if (mc.options.hudHidden) return;
		drawBar(ctx, mc, w, h, time);
		drawThought(ctx, mc, w, h, tickDelta);
	}

	/** Тьма по краям экрана: чем выше шкала, тем сильнее виньетка и общее затемнение. */
	private static void drawAtmosphere(DrawContext ctx, int w, int h, float time) {
		float t = MathHelper.clamp((DepressionClient.shown - 300f) / 2100f, 0f, 1f);
		if (t <= 0.01f) return;
		float pulse = DepressionClient.shown >= DepressionManager.LEVEL_3
			? 1f + 0.06f * MathHelper.sin(time * 0.12f) : 1f;

		float vignette = MathHelper.clamp((0.15f + 0.85f * t) * pulse, 0f, 1f);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1f, 1f, 1f, vignette);
		ctx.drawTexture(VIGNETTE, 0, 0, w, h, 0f, 0f, 512, 512, 512, 512);
		if (t > 0.6f) { // на высоких уровнях тьма сгущается второй раз
			RenderSystem.setShaderColor(1f, 1f, 1f, (t - 0.6f) / 0.4f * 0.6f);
			ctx.drawTexture(VIGNETTE, 0, 0, w, h, 0f, 0f, 512, 512, 512, 512);
		}
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		int dim = (int) (t * 0.30f * 255f);
		if (dim > 0) ctx.fill(0, 0, w, h, dim << 24);
		RenderSystem.disableBlend();
	}

	private static void drawBar(DrawContext ctx, MinecraftClient mc, int w, int h, float time) {
		// Ровно над рядом голода (h - 39); если показан воздух, поднимаемся ещё на ряд.
		int y = h - 39 - 10;
		if (mc.player.isSubmergedIn(FluidTags.WATER) || mc.player.getAir() < mc.player.getMaxAir()) {
			y -= 10;
		}
		boolean joy = mc.player.hasStatusEffect(ModEffects.JOY);
		int value = DepressionClient.value;
		int lvl = DepressionManager.level(value);
		float frac = MathHelper.clamp(DepressionClient.shown / DepressionManager.MAX, 0f, 1f);

		// Иконка у правого края (там же, где крайняя иконка голода).
		int iconX = w / 2 + 82;
		int frame = joy ? 4 : lvl; // 0..3 по уровню, 4 — радость
		int shake = (lvl == 3 && !joy && ((int) (time / 2f)) % 7 == 0) ? 1 : 0;
		ctx.drawTexture(MOOD, iconX + shake, y, frame * 9, 0, 9, 9, 45, 9);

		// Полоска.
		int x0 = w / 2 + 10;
		int by = y + 2;
		int border = joy ? 0xFFE8C14A : 0xFF000000;
		ctx.fill(x0 - 1, by - 1, x0 + BAR_WIDTH + 1, by + 6, border);
		ctx.fill(x0, by, x0 + BAR_WIDTH, by + 5, 0xFF24242C);

		int fw = Math.round(BAR_WIDTH * frac);
		if (fw > 0) {
			int color = barColor(frac);
			ctx.fill(x0, by, x0 + fw, by + 5, color);
			ctx.fill(x0, by, x0 + fw, by + 1, lighten(color)); // блик сверху
		}
		// Отметки уровней: 25 %, 50 %, 75 %.
		for (int i = 1; i <= 3; i++) {
			int tx = x0 + BAR_WIDTH * i / 4;
			ctx.fill(tx, by, tx + 1, by + 5, 0x90000000);
		}
	}

	private static int barColor(float f) {
		// голубой → фиолетовый → тёмно-багровый
		int[] a = {0x7F, 0xA8, 0xD8};
		int[] b = {0x6A, 0x3F, 0xA8};
		int[] c = {0x8A, 0x1F, 0x3D};
		int[] from = f < 0.5f ? a : b;
		int[] to = f < 0.5f ? b : c;
		float k = f < 0.5f ? f * 2f : (f - 0.5f) * 2f;
		int r = (int) (from[0] + (to[0] - from[0]) * k);
		int g = (int) (from[1] + (to[1] - from[1]) * k);
		int bl = (int) (from[2] + (to[2] - from[2]) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	private static int lighten(int c) {
		int r = Math.min(255, ((c >> 16) & 0xFF) + 40);
		int g = Math.min(255, ((c >> 8) & 0xFF) + 40);
		int b = Math.min(255, (c & 0xFF) + 40);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	/** Короткие фразы над хотбаром: плавно появляются и исчезают. */
	private static void drawThought(DrawContext ctx, MinecraftClient mc, int w, int h, float tickDelta) {
		Text text = DepressionClient.thought;
		if (text == null) return;
		float age = DepressionClient.thoughtAge + tickDelta;
		float total = DepressionClient.THOUGHT_TICKS;
		float alpha;
		if (age < 20f) alpha = age / 20f;
		else if (age > total - 35f) alpha = (total - age) / 35f;
		else alpha = 1f;
		int a = (int) (MathHelper.clamp(alpha, 0f, 1f) * 255f);
		if (a < 12) return; // при очень малой прозрачности шрифт рисуется непрозрачным

		TextRenderer font = mc.textRenderer;
		Text styled = text.copy().formatted(Formatting.ITALIC);
		int color = (a << 24) | 0xC4C4DA;
		ctx.drawCenteredTextWithShadow(font, styled, w / 2, h - 80, color);
	}
}
