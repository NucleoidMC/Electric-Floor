package io.github.haykam821.electricfloor.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ElectricFloorMapConfig {
	public static final Codec<ElectricFloorMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(map -> map.x),
			Codec.INT.fieldOf("z").forGetter(map -> map.z)
		).apply(instance, ElectricFloorMapConfig::new);
	});

	public final int x;
	public final int z;

	public ElectricFloorMapConfig(int x, int z) {
		this.x = x;
		this.z = z;
	}
}