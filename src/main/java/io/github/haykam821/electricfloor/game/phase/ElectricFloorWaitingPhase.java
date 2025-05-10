package io.github.haykam821.electricfloor.game.phase;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorGuideText;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElectricFloorWaitingPhase {
	private static final int NIGHT_TICKS = SharedConstants.TICKS_PER_MINUTE * 15;

	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;

	private HolderAttachment guideText;

	public ElectricFloorWaitingPhase(GameSpace gameSpace, ServerWorld world, ElectricFloorMap map, ElectricFloorConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ElectricFloorConfig> context) {
		ElectricFloorConfig config = context.config();

		ElectricFloorMapBuilder mapBuilder = new ElectricFloorMapBuilder(config);
		ElectricFloorMap map = mapBuilder.create();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		if (config.isNight()) {
			worldConfig.setTimeOfDay(NIGHT_TICKS);
		}

		return context.openWithWorld(worldConfig, (activity, world) -> {
			ElectricFloorWaitingPhase phase = new ElectricFloorWaitingPhase(activity.getGameSpace(), world, map, config);

			GameWaitingLobby.addTo(activity, config.getPlayerConfig());
			ElectricFloorActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private void enable() {
		// Spawn guide text
		Vec3d guideTextPos = this.map.getGuideTextPos();

		if (guideTextPos != null) {
			ElementHolder holder = ElectricFloorGuideText.createElementHolder(this.config.isNight());
			this.guideText = ChunkAttachment.of(holder, world, guideTextPos);
		}
	}

	public GameResult requestStart() {
		ElectricFloorActivePhase.open(this.gameSpace, this.world, this.map, this.config, this.guideText);
		return GameResult.ok();
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			player.changeGameMode(GameMode.ADVENTURE);
		});
	}

	public EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player at the start
		this.map.teleportToWaitingSpawn(player, this.world);
		return EventResult.ALLOW;
	}
}
