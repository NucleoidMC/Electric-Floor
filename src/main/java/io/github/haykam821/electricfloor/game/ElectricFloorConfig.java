package io.github.haykam821.electricfloor.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.gegy1000.plasmid.game.config.GameConfig;
import net.gegy1000.plasmid.game.config.GameMapConfig;
import net.gegy1000.plasmid.game.config.PlayerConfig;

public class ElectricFloorConfig implements GameConfig {
	public static final Codec<ElectricFloorConfig> CODEC = RecordCodecBuilder.create(instance -> {
		Codec<GameMapConfig<ElectricFloorConfig>> mapCodec = GameMapConfig.codec();
		return instance.group(
			mapCodec.fieldOf("map").forGetter(ElectricFloorConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(ElectricFloorConfig::getPlayerConfig),
			Codec.INT.optionalFieldOf("delay", 5).forGetter(ElectricFloorConfig::getDelay)
		).apply(instance, ElectricFloorConfig::new);
	});

	private final GameMapConfig<ElectricFloorConfig> mapConfig;
	private final PlayerConfig playerConfig;
	private final int delay;

	public ElectricFloorConfig(GameMapConfig<ElectricFloorConfig> mapConfig, PlayerConfig playerConfig, int delay) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.delay = delay;
	}

	public GameMapConfig<ElectricFloorConfig> getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public int getDelay() {
		return this.delay;
	}
}