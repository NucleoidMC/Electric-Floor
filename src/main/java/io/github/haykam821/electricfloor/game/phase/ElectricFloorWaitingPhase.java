package io.github.haykam821.electricfloor.game.phase;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapBuilder;
import net.gegy1000.plasmid.game.GameWorld;
import net.gegy1000.plasmid.game.GameWorldState;
import net.gegy1000.plasmid.game.StartResult;
import net.gegy1000.plasmid.game.config.PlayerConfig;
import net.gegy1000.plasmid.game.event.OfferPlayerListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.RequestStartListener;
import net.gegy1000.plasmid.game.player.JoinResult;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ElectricFloorWaitingPhase {
	private final GameWorld gameWorld;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;

	public ElectricFloorWaitingPhase(GameWorld gameWorld, ElectricFloorMap map, ElectricFloorConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<Void> open(GameWorldState gameState, ElectricFloorConfig config) {
		ElectricFloorMapBuilder mapBuilder = new ElectricFloorMapBuilder(config);

		return mapBuilder.create().thenAccept(map -> {
			GameWorld gameWorld = gameState.openWorld(map.createGenerator());
			ElectricFloorWaitingPhase phase = new ElectricFloorWaitingPhase(gameWorld, map, config);

			gameWorld.newGame(game -> {
				ElectricFloorActivePhase.setRules(game);

				// Listeners
				game.on(PlayerAddListener.EVENT, phase::addPlayer);
				game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
				game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
				game.on(RequestStartListener.EVENT, phase::requestStart);
			});
		});
	}

	private boolean isFull() {
		return this.gameWorld.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameWorld.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.notEnoughPlayers();
		}

		ElectricFloorActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.ok();
	}

	public void addPlayer(ServerPlayerEntity player) {
		ElectricFloorActivePhase.spawn(this.gameWorld.getWorld(), this.map, player);
	}

	public boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		ElectricFloorActivePhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return true;
	}
}