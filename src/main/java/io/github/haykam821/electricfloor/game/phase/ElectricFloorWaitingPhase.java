package io.github.haykam821.electricfloor.game.phase;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;

public class ElectricFloorWaitingPhase {
	private final GameSpace gameSpace;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;

	public ElectricFloorWaitingPhase(GameSpace gameSpace, ElectricFloorMap map, ElectricFloorConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ElectricFloorConfig> context) {
		ElectricFloorMapBuilder mapBuilder = new ElectricFloorMapBuilder(context.getConfig());
		ElectricFloorMap map = mapBuilder.create();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			ElectricFloorWaitingPhase phase = new ElectricFloorWaitingPhase(game.getSpace(), map, context.getConfig());

			GameWaitingLobby.applyTo(game, context.getConfig().getPlayerConfig());
			ElectricFloorActivePhase.setRules(game);

			// Listeners
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(RequestStartListener.EVENT, phase::requestStart);
		});
	}

	public StartResult requestStart() {
		ElectricFloorActivePhase.open(this.gameSpace, this.map, this.config);
		return StartResult.OK;
	}

	public void addPlayer(ServerPlayerEntity player) {
		this.spawnPlayer(player);
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		this.spawnPlayer(player);
		return ActionResult.SUCCESS;
	}

	private void spawnPlayer(ServerPlayerEntity player) {
		Vec3d center = this.map.getPlatform().getCenter();
		player.teleport(this.gameSpace.getWorld(), center.getX(), center.getY() + 0.5, center.getZ(), 0, 0);
	}
}
