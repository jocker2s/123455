package com.example.depression;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Дневник: можно «писать» раз в игровые сутки и получить небольшое облегчение. */
public class DiaryItem extends Item {
	public static final int RELIEF = 400;

	public DiaryItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);

		if (!world.isClient && user instanceof ServerPlayerEntity player) {
			MinecraftServer server = player.getServer();
			DepressionState state = DepressionManager.state(server);
			long today = server.getOverworld().getTimeOfDay() / 24000L;

			if (state.getDiaryDay(player.getUuid()) == today) {
				player.sendMessage(Text.translatable("message.depression.diary.done"), true);
			} else {
				state.setDiaryDay(player.getUuid(), today);
				DepressionManager.adjust(player, -RELIEF);
				world.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 0.9f);
				player.sendMessage(Text.translatable("message.depression.diary.written"), true);
			}
			player.getItemCooldownManager().set(this, 40);
		}
		return TypedActionResult.success(stack, world.isClient());
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("item.depression.diary.tooltip").formatted(Formatting.GRAY));
	}
}
