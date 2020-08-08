package io.github.haykam821.electricfloor.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.electricfloor.game.map.ElectricFloorMapConfig;
import net.gegy1000.plasmid.game.config.GameConfig;
import net.gegy1000.plasmid.game.config.PlayerConfig;

public class ElectricFloorConfig implements GameConfig {
	public static final Codec<ElectricFloorConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			ElectricFloorMapConfig.CODEC.fieldOf("map").forGetter(ElectricFloorConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(ElectricFloorConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("delay", 5).forGetter(ElectricFloorConfig::getDelay)
		).apply(instance, ElectricFloorConfig::new);
	});

	private final ElectricFloorMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final int delay;

	public ElectricFloorConfig(ElectricFloorMapConfig mapConfig, PlayerConfig playerConfig, int delay) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.delay = delay;
	}

	public ElectricFloorMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getDelay() {
		return this.delay;
	}
}