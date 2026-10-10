package com.example.depression.mixin;

import com.example.depression.DepressionManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	@Unique
	private int depression$foodBefore;

	/** Апатия: на высоких уровнях блоки добываются медленнее. */
	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void depression$apathy(BlockState block, CallbackInfoReturnable<Float> cir) {
		int lvl = DepressionManager.effectLevel((PlayerEntity) (Object) this);
		if (lvl >= 2) {
			cir.setReturnValue(cir.getReturnValueF() * (lvl == 2 ? 0.8f : 0.55f));
		}
	}

	/** Потеря аппетита: запоминаем сытость до еды... */
	@Inject(method = "eatFood", at = @At("HEAD"))
	private void depression$foodStart(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		depression$foodBefore = ((PlayerEntity) (Object) this).getHungerManager().getFoodLevel();
	}

	/** ...и забираем часть восстановленного голода. */
	@Inject(method = "eatFood", at = @At("RETURN"))
	private void depression$foodEnd(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		int lvl = DepressionManager.effectLevel(self);
		if (lvl < 2) return;
		int gained = self.getHungerManager().getFoodLevel() - depression$foodBefore;
		if (gained <= 0) return;
		int lost = (int) (gained * (lvl == 2 ? 0.25f : 0.5f));
		if (lost > 0) {
			self.getHungerManager().setFoodLevel(self.getHungerManager().getFoodLevel() - lost);
		}
	}
}
