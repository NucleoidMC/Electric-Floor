package io.github.haykam821.electricfloor.game.phase;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.world.bubble.BubbleWorldConfig;

public class ElectricFloorWaitingPhase {
	private final GameWorld gameWorld;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;

	public ElectricFloorWaitingPhase(GameWorld gameWorld, ElectricFloorMap map, ElectricFloorConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<Void> open(MinecraftServer server, ElectricFloorConfig config) {
		ElectricFloorMapBuilder mapBuilder = new ElectricFloorMapBuilder(config);

		return mapBuilder.create().thenAccept(map -> {
			BubbleWorldConfig worldConfig = new BubbleWorldConfig()
				.setGenerator(map.createGenerator(server))
				.setDefaultGameMode(GameMode.ADVENTURE);
			GameWorld gameWorld = GameWorld.open(server, worldConfig);

			ElectricFloorWaitingPhase phase = new ElectricFloorWaitingPhase(gameWorld, map, config);

			gameWorld.openGame(game -> {
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

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		ElectricFloorActivePhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return ActionResult.SUCCESS;
	}
}