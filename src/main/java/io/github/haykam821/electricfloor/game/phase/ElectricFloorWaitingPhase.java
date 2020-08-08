package io.github.haykam821.electricfloor.game.phase;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import net.gegy1000.plasmid.game.Game;
import net.gegy1000.plasmid.game.JoinResult;
import net.gegy1000.plasmid.game.StartResult;
import net.gegy1000.plasmid.game.config.PlayerConfig;
import net.gegy1000.plasmid.game.event.OfferPlayerListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.RequestStartListener;
import net.gegy1000.plasmid.game.map.GameMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ElectricFloorWaitingPhase {
	private final ElectricFloorConfig config;

	public ElectricFloorWaitingPhase(GameMap map, ElectricFloorConfig config) {
		this.config = config;
	}

	public static Game open(GameMap map, ElectricFloorConfig config) {
		ElectricFloorWaitingPhase game = new ElectricFloorWaitingPhase(map, config);

		Game.Builder builder = Game.builder();
		builder.setMap(map);

		ElectricFloorActivePhase.setRules(builder);

		// Listeners
		builder.on(PlayerAddListener.EVENT, game::addPlayer);
		builder.on(PlayerDeathListener.EVENT, game::onPlayerDeath);
		builder.on(OfferPlayerListener.EVENT, game::offerPlayer);
		builder.on(RequestStartListener.EVENT, game::requestStart);

		return builder.build();
	}

	private boolean isFull(Game game) {
		return game.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	public JoinResult offerPlayer(Game game, ServerPlayerEntity player) {
		return this.isFull(game) ? JoinResult.gameFull() : JoinResult.ok();
	}

	public StartResult requestStart(Game game) {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (game.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.notEnoughPlayers();
		}

		Game activeGame = ElectricFloorActivePhase.open(game.getMap(), this.config, game.getPlayers());
		return StartResult.ok(activeGame);
	}

	public void addPlayer(Game game, ServerPlayerEntity player) {
		ElectricFloorActivePhase.spawn(game.getMap(), player);
	}

	public boolean onPlayerDeath(Game game, ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		ElectricFloorActivePhase.spawn(game.getMap(), player);
		return true;
	}
}