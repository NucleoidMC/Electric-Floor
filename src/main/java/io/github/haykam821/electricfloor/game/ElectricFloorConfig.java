package io.github.haykam821.electricfloor.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.electricfloor.game.map.ElectricFloorMapConfig;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class ElectricFloorConfig {
	public static final Codec<ElectricFloorConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			ElectricFloorMapConfig.CODEC.fieldOf("map").forGetter(ElectricFloorConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(ElectricFloorConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("spawn_platform_delay", 20 * 2).forGetter(ElectricFloorConfig::getSpawnPlatformDelay),
			Codec.INT.optionalFieldOf("delay", 5).forGetter(ElectricFloorConfig::getDelay)
		).apply(instance, ElectricFloorConfig::new);
	});

	private final ElectricFloorMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int spawnPlatformDelay;
	private final int delay;

	public ElectricFloorConfig(ElectricFloorMapConfig mapConfig, PlayerConfig playerConfig, int spawnPlatformDelay, int delay) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.spawnPlatformDelay = spawnPlatformDelay;
		this.delay = delay;
	}

	public ElectricFloorMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getSpawnPlatformDelay() {
		return this.spawnPlatformDelay;
	}

	public int getDelay() {
		return this.delay;
	}
}