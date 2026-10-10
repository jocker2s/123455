package com.example.depression.mixin;

import com.example.depression.DepressionManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
	/** Жители замыкаются: на последнем уровне торговля идёт хуже (цены выше на ~30%). */
	@Inject(method = "prepareOffersFor", at = @At("TAIL"))
	private void depression$withdrawn(PlayerEntity player, CallbackInfo ci) {
		if (DepressionManager.effectLevel(player) < 3) return;
		for (TradeOffer offer : ((VillagerEntity) (Object) this).getOffers()) {
			int base = offer.getOriginalFirstBuyItem().getCount();
			offer.increaseSpecialPrice(Math.max(1, Math.round(base * 0.3f)));
		}
	}
}
