package com.example.depression;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** /depression get | /depression set <0..2400> — для проверки и отладки. */
public final class DepressionCommand {
	private DepressionCommand() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("depression")
			.requires(src -> src.hasPermissionLevel(2))
			.then(CommandManager.literal("get").executes(ctx -> {
				ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
				int v = DepressionManager.state(ctx.getSource().getServer()).get(p.getUuid());
				ctx.getSource().sendFeedback(() -> Text.literal(
					"Depression: " + v + "/" + DepressionManager.MAX
						+ " (level " + DepressionManager.level(v) + ")"), false);
				return v;
			}))
			.then(CommandManager.literal("set")
				.then(CommandManager.argument("value",
						IntegerArgumentType.integer(0, DepressionManager.MAX))
					.executes(ctx -> {
						ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
						int value = IntegerArgumentType.getInteger(ctx, "value");
						DepressionManager.set(p, value);
						ctx.getSource().sendFeedback(() -> Text.literal(
							"Depression set to " + value), false);
						return value;
					}))));
	}
}
