package io.github.haykam821.electricfloor.game.phase;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.electricfloor.Main;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKeys;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElectricFloorActivePhase {
	private final ServerLevel level;
	private final GameSpace gameSpace;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;
	private final GameStatisticBundle statistics;
	private final Set<ServerPlayer> players = new HashSet<>();
	private HolderAttachment guideText;
	private boolean singleplayer;
	private final Long2IntMap convertPositions = new Long2IntOpenHashMap();
	private int timeElapsed = 0;
	private int ticksUntilClose = -1;

	public ElectricFloorActivePhase(GameSpace gameSpace, ServerLevel level, ElectricFloorMap map, ElectricFloorConfig config, HolderAttachment guideText) {
		this.level = level;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		this.guideText = guideText;

		this.statistics = config.getStatisticBundle(gameSpace);
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
	}

	public static void open(GameSpace gameSpace, ServerLevel level, ElectricFloorMap map, ElectricFloorConfig config, HolderAttachment guideText) {
		gameSpace.setActivity(activity -> {
			ElectricFloorActivePhase phase = new ElectricFloorActivePhase(gameSpace, level, map, config, guideText);
			gameSpace.getPlayers().participants().forEach(phase.players::add);

			ElectricFloorActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
		});
	}

	public void enable() {
		this.singleplayer = this.players.size() == 1;

		ElectricFloorMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.x, mapConfig.z) - 4) / 2;

		Vec3 center = this.map.getPlatform().center();

		int index = 0;
 		for (ServerPlayer player : this.players) {
			player.setGameMode(GameType.ADVENTURE);

			if (!this.singleplayer && this.statistics != null) {
				this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
			}

			double theta = ((double) index++ / this.players.size()) * 2 * Math.PI;
			double x = center.x() + Math.sin(theta) * spawnRadius;
			double z = center.z() + Math.cos(theta) * spawnRadius;

			player.teleportTo(this.level, x, 1, z, Set.of(), (float) theta - 180, 0, true);

			// Create spawn platform
			for (BlockPos pos : BlockPos.betweenClosed((int) x - 1, 0, (int) z - 1, (int) x, 0, (int) z)) {
				this.setBlockState(pos, Main.SPAWN_PLATFORM.defaultBlockState());
				this.convertPositions.putIfAbsent(pos.asLong(), this.config.getSpawnPlatformDelay());
			}
		}

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			this.map.teleportToWaitingSpawn(player, this.level);
			this.setSpectator(player);
		}
	}

	public void tick() {
		this.timeElapsed += 1;

		if (this.guideText != null) {
			if (this.timeElapsed == this.config.getGuideTicks()) {
				this.guideText.destroy();
				this.guideText = null;
			}
		}

		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

 		ObjectIterator<Long2IntMap.Entry> iterator = Long2IntMaps.fastIterator(this.convertPositions);
		while (iterator.hasNext()) {
			Long2IntMap.Entry entry = iterator.next();
			long convertPos = entry.getLongKey();
			int ticksLeft = entry.getIntValue();

			if (ticksLeft == 0) {
				pos.set(convertPos);
		
				BlockState state = this.level.getBlockState(pos);
				this.setBlockState(pos, Main.getConvertedFloor(state));

				iterator.remove();
			} else {
				entry.setValue(ticksLeft - 1);
			}
		}

		Iterator<ServerPlayer> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			ServerPlayer player = playerIterator.next();
			if (!this.map.getBox().contains(player.position())) {
				this.eliminate(player, false);
				playerIterator.remove();
			}

			BlockPos steppingPos = player.getOnPos();
			BlockState state = this.level.getBlockState(steppingPos);

			if (Main.isConvertible(state)) {
				BlockState convertedState = Main.getConvertedFloor(state);
				if (convertedState == null) {
					this.eliminate(player, false);
					playerIterator.remove();
				} else {
					long steppingPosKey = steppingPos.asLong();
					if (!this.convertPositions.containsKey(steppingPosKey)) {
						if (!this.singleplayer && this.statistics != null) {
							this.statistics.forPlayer(player).increment(Main.BLOCKS_CONVERTED, 1);
						}
						this.convertPositions.put(steppingPosKey, this.config.getDelay());
					}
				}
			}
		}

		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			ServerPlayer winner = this.getWinner();
			if (winner != null) {
				this.applyPlayerFinishStatistics(winner, StatisticKeys.GAMES_WON);
			}

			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage(winner));
			this.gameSpace.getPlayers().playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.UI, 1, 1);
			this.endGame();
		}
	}

	private ServerPlayer getWinner() {
		if (this.players.size() == 1) {
			return this.players.iterator().next();
		}
		return null;
	}

	private Component getEndingMessage(ServerPlayer winner) {
		if (winner != null) {
			return Component.translatable("text.electricfloor.win", winner.getDisplayName()).withStyle(ChatFormatting.GOLD);
		}
		return Component.translatable("text.electricfloor.no_winners").withStyle(ChatFormatting.GOLD);
	}

	private void setSpectator(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);
	}

	public JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, this.map.getSpectatorSpawnPos()).thenRunForEach(player -> {
			this.setSpectator(player);
		});
	}

	public void removePlayer(ServerPlayer player) {
		this.eliminate(player, true);
	}

	public void eliminate(ServerPlayer eliminatedPlayer, boolean remove) {
		if (this.isGameEnding()) return;
		if (!this.players.contains(eliminatedPlayer)) return;

		Component message = Component.translatable("text.electricfloor.eliminated", eliminatedPlayer.getDisplayName()).withStyle(ChatFormatting.RED);
		for (ServerPlayer player : this.gameSpace.getPlayers()) {
			player.sendSystemMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer);

		this.applyPlayerFinishStatistics(eliminatedPlayer, StatisticKeys.GAMES_LOST);
	}

	public void applyPlayerFinishStatistics(ServerPlayer player, StatisticKey<Integer> finishTypeKey) {
		if (!this.singleplayer && this.statistics != null) {
			this.statistics.forPlayer(player).increment(finishTypeKey, 1);
			this.statistics.forPlayer(player).set(StatisticKeys.LONGEST_TIME, this.timeElapsed);
		}
	}

	public EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		this.eliminate(player, true);
		return EventResult.ALLOW;
	}

	private void endGame() {
		this.ticksUntilClose = this.config.getTicksUntilClose().sample(this.level.getRandom());
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	private void setBlockState(BlockPos pos, BlockState state) {
		this.level.setBlockAndUpdate(pos, state);

		BlockState lightState = Main.getFloorLightState(state, this.config.isNight());

		if (lightState != null) {
			this.level.setBlockAndUpdate(pos.above(), lightState);
		}
	}
}
