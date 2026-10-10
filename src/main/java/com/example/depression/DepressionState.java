package com.example.depression;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Хранит «шкалу грусти» каждого игрока в мире, чтобы она переживала перезапуск. */
public class DepressionState extends PersistentState {
	private final Map<UUID, Integer> values = new HashMap<>();
	/** Сколько секунд подряд шкала держится около нуля. */
	private final Map<UUID, Integer> calm = new HashMap<>();
	/** В какие игровые сутки игрок последний раз писал в дневник. */
	private final Map<UUID, Long> diary = new HashMap<>();

	public int get(UUID id) {
		return values.getOrDefault(id, 0);
	}

	public void set(UUID id, int value) {
		if (get(id) != value) {
			values.put(id, value);
			markDirty();
		}
	}

	public int getCalm(UUID id) {
		return calm.getOrDefault(id, 0);
	}

	public void setCalm(UUID id, int seconds) {
		if (getCalm(id) != seconds) {
			calm.put(id, seconds);
			markDirty();
		}
	}

	public long getDiaryDay(UUID id) {
		return diary.getOrDefault(id, -1L);
	}

	public void setDiaryDay(UUID id, long day) {
		diary.put(id, day);
		markDirty();
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtCompound players = new NbtCompound();
		values.forEach((id, v) -> players.putInt(id.toString(), v));
		nbt.put("players", players);

		NbtCompound calmTag = new NbtCompound();
		calm.forEach((id, v) -> calmTag.putInt(id.toString(), v));
		nbt.put("calm", calmTag);

		NbtCompound diaryTag = new NbtCompound();
		diary.forEach((id, v) -> diaryTag.putLong(id.toString(), v));
		nbt.put("diary", diaryTag);
		return nbt;
	}

	public static DepressionState fromNbt(NbtCompound nbt) {
		DepressionState state = new DepressionState();
		NbtCompound players = nbt.getCompound("players");
		for (String key : players.getKeys()) {
			try {
				state.values.put(UUID.fromString(key), players.getInt(key));
			} catch (IllegalArgumentException ignored) {
				// битая запись — пропускаем
			}
		}
		NbtCompound calmTag = nbt.getCompound("calm");
		for (String key : calmTag.getKeys()) {
			try {
				state.calm.put(UUID.fromString(key), calmTag.getInt(key));
			} catch (IllegalArgumentException ignored) {
			}
		}
		NbtCompound diaryTag = nbt.getCompound("diary");
		for (String key : diaryTag.getKeys()) {
			try {
				state.diary.put(UUID.fromString(key), diaryTag.getLong(key));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return state;
	}
}
