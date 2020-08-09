package io.github.haykam821.electricfloor.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ElectricFloorMapConfig {
	public static final Codec<ElectricFloorMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(map -> map.x),
			Codec.INT.fieldOf("z").forGetter(map -> map.z),
			Codec.BOOL.optionalFieldOf("walls", true).forGetter(ElectricFloorMapConfig::hasWalls)
		).apply(instance, ElectricFloorMapConfig::new);
	});

	public final int x;
	public final int z;
	private final boolean walls;

	public ElectricFloorMapConfig(int x, int z, boolean walls) {
		this.x = x;
		this.z = z;
		this.walls = walls;
	}

	public boolean hasWalls() {
		return this.walls;
	}
}