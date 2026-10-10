package com.example.depression;

import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Награда за долгое хорошее настроение: чуть быстрее ходьба и чуть больше удачи. */
public class JoyEffect extends StatusEffect {
	public JoyEffect() {
		super(StatusEffectCategory.BENEFICIAL, 0xF2C94C);

		addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED,
			"d6b1f2a4-7c3e-4a95-8e10-5f2b9c4d6e55", 0.08,
			EntityAttributeModifier.Operation.MULTIPLY_TOTAL);

		addAttributeModifier(EntityAttributes.GENERIC_LUCK,
			"e7c2a3b5-8d4f-4ba6-9f21-6a3c0d5e7f66", 1.0,
			EntityAttributeModifier.Operation.ADDITION);
	}
}
