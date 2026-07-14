package io.github.haykam821.electricfloor.game.phase;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorGuideText;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
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
	private final ServerLevel world;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;

	private HolderAttachment guideText;

	public ElectricFloorWaitingPhase(GameSpace gameSpace, ServerLevel level, ElectricFloorMap map, ElectricFloorConfig config) {
		this.gameSpace = gameSpace;
		this.world = level;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<ElectricFloorConfig> context) {
		ElectricFloorConfig config = context.config();

		ElectricFloorMapBuilder mapBuilder = new ElectricFloorMapBuilder(config);
		ElectricFloorMap map = mapBuilder.create();

		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setGenerator(map.createGenerator(context.server()));

		if (config.isNight()) {
			//levelConfig.setTimeOfDay(NIGHT_TICKS);
		}

		return context.openWithLevel(levelConfig, (activity, level) -> {
			ElectricFloorWaitingPhase phase = new ElectricFloorWaitingPhase(activity.getGameSpace(), level, map, config);

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
		Vec3 guideTextPos = this.map.getGuideTextPos();

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
			player.setGameMode(GameType.ADVENTURE);
		});
	}

	public EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		// Respawn player at the start
		this.map.teleportToWaitingSpawn(player, this.world);
		return EventResult.ALLOW;
	}
}
