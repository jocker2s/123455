package com.example.depression;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * Эффект «Депрессия». Чем выше уровень, тем сильнее штрафы:
 * медленнее ходьба, слабее удар, меньше максимальное здоровье.
 */
public class DepressionEffect extends StatusEffect {
	public DepressionEffect() {
		super(StatusEffectCategory.HARMFUL, 0x3B4A6B);

		addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
			"5d1a7c3e-2b64-4f0a-9c1d-7e8a3b9f0a11", -0.08,
			EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED,
			"8c2f4b6d-1e93-47a5-b0d2-4a6c8e1f3b22", -0.10,
			EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

		addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,
			"3e7d9a1b-6c58-4d2e-8f40-1b3d5f7a9c33", -0.5,
			EntityAttributeModifier.Operation.ADDITION);

		addAttributeModifier(EntityAttributes.GENERIC_MAX_HEALTH,
			"b4a6c8e0-9d17-4b3f-a52c-6e8f0a2b4d44", -2.0,
			EntityAttributeModifier.Operation.ADDITION);
	}
}
