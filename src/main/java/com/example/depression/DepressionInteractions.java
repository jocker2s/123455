package com.example.depression;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

/** Забота: питомцы и подарки другим игрокам. */
public final class DepressionInteractions {
	public static final int RELIEF_FEED = 150;
	public static final int RELIEF_PET = 100;
	public static final int RELIEF_GIFT = 300;
	private static final int COOLDOWN_PET = 600;   // 30 секунд
	private static final int COOLDOWN_GIFT = 2400; // 2 минуты

	private DepressionInteractions() {}

	public static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand,
										   Entity entity, EntityHitResult hit) {
		if (world.isClient || !(player instanceof ServerPlayerEntity sp) || player.isSpectator()) {
			return ActionResult.PASS;
		}
		if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
		ItemStack stack = player.getStackInHand(hand);
		int now = sp.getServer().getTicks();

		// Свой питомец.
		if (entity instanceof TameableEntity pet && pet.isOwner(player)) {
			if (!stack.isEmpty()) {
				// Покормить: пока питомец ранен и это его еда — ванильная логика сама его накормит.
				if (pet.isBreedingItem(stack) && pet.getHealth() < pet.getMaxHealth()
					&& DepressionManager.tryCooldown(sp.getUuid(), "feed", now, COOLDOWN_PET)) {
					hearts(sp.getServerWorld(), pet);
					DepressionManager.adjust(sp, -RELIEF_FEED);
				}
				return ActionResult.PASS;
			}
			// Погладить: пустая рука + присесть (чтобы не путать с командой «сидеть»).
			if (player.isSneaking()) {
				if (DepressionManager.tryCooldown(sp.getUuid(), "pet", now, COOLDOWN_PET)) {
					hearts(sp.getServerWorld(), pet);
					DepressionManager.adjust(sp, -RELIEF_PET);
				}
				return ActionResult.SUCCESS;
			}
			return ActionResult.PASS;
		}

		// Подарить цветок другому игроку.
		if (entity instanceof ServerPlayerEntity target && target != sp && !target.isSpectator()
			&& stack.isIn(ItemTags.FLOWERS)) {
			if (!DepressionManager.tryCooldown(sp.getUuid(), "gift", now, COOLDOWN_GIFT)) {
				return ActionResult.SUCCESS;
			}
			if (!sp.isCreative()) stack.decrement(1);
			hearts(sp.getServerWorld(), target);
			DepressionManager.adjust(sp, -RELIEF_GIFT);
			DepressionManager.adjust(target, -RELIEF_GIFT);
			sp.sendMessage(Text.translatable("message.depression.gift.given", target.getDisplayName()), true);
			target.sendMessage(Text.translatable("message.depression.gift.received", sp.getDisplayName()), true);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}

	private static void hearts(ServerWorld world, Entity e) {
		world.spawnParticles(ParticleTypes.HEART, e.getX(), e.getBodyY(0.9), e.getZ(),
			5, 0.3, 0.3, 0.3, 0.02);
	}
}
