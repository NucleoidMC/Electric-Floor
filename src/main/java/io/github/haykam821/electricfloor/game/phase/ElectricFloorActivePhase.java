package io.github.haykam821.electricfloor.game.phase;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import io.github.haykam821.electricfloor.Main;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class ElectricFloorActivePhase {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;
	private final GameStatisticBundle statistics;
	private final Set<ServerPlayerEntity> players = new HashSet<>();
	private boolean singleplayer;
	private final Long2IntMap convertPositions = new Long2IntOpenHashMap();
	private int timeElapsed = 0;
	private int ticksUntilClose = -1;

	public ElectricFloorActivePhase(GameSpace gameSpace, ServerWorld world, ElectricFloorMap map, ElectricFloorConfig config) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;

		this.statistics = config.getStatisticBundle(gameSpace);
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, ElectricFloorMap map, ElectricFloorConfig config) {
		gameSpace.setActivity(activity -> {
			ElectricFloorActivePhase phase = new ElectricFloorActivePhase(gameSpace, world, map, config);
			gameSpace.getPlayers().forEach(phase.players::add);

			ElectricFloorActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
		});
	}

	public void enable() {
		this.singleplayer = this.players.size() == 1;

		ElectricFloorMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.x, mapConfig.z) - 4) / 2;

		Vec3d center = this.map.getPlatform().center();

		int index = 0;
 		for (ServerPlayerEntity player : this.players) {
			player.changeGameMode(GameMode.ADVENTURE);

			if (!this.singleplayer && this.statistics != null) {
				this.statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
			}

			double theta = ((double) index++ / this.players.size()) * 2 * Math.PI;
			double x = center.getX() + Math.sin(theta) * spawnRadius;
			double z = center.getZ() + Math.cos(theta) * spawnRadius;

			player.teleport(this.world, x, 1, z, (float) theta - 180, 0);

			// Create spawn platform
			for (BlockPos pos : BlockPos.iterate((int) x - 1, 0, (int) z - 1, (int) x, 0, (int) z)) {
				this.world.setBlockState(pos, Main.SPAWN_PLATFORM.getDefaultState());
				this.convertPositions.putIfAbsent(pos.asLong(), this.config.getSpawnPlatformDelay());
			}
		}
	}

	public void tick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		this.timeElapsed += 1;

		BlockPos.Mutable pos = new BlockPos.Mutable();

 		ObjectIterator<Long2IntMap.Entry> iterator = Long2IntMaps.fastIterator(this.convertPositions);
		while (iterator.hasNext()) {
			Long2IntMap.Entry entry = iterator.next();
			long convertPos = entry.getLongKey();
			int ticksLeft = entry.getIntValue();

			if (ticksLeft == 0) {
				pos.set(convertPos);
		
				BlockState state = this.world.getBlockState(pos);
				this.world.setBlockState(pos, Main.getConvertedFloor(state));

				iterator.remove();
			} else {
				entry.setValue(ticksLeft - 1);
			}
		}

		Iterator<ServerPlayerEntity> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			ServerPlayerEntity player = playerIterator.next();
			if (!this.map.getBox().contains(player.getPos())) {
				this.eliminate(player, false);
				playerIterator.remove();
			}

			BlockPos steppingPos = player.getSteppingPos();
			BlockState state = this.world.getBlockState(steppingPos);

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
			
			ServerPlayerEntity winner = this.getWinner();
			if (winner != null) {
				this.applyPlayerFinishStatistics(winner, StatisticKeys.GAMES_WON);
			}

			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage(winner));

			this.endGame();
		}
	}

	private ServerPlayerEntity getWinner() {
		if (this.players.size() == 1) {
			return this.players.iterator().next();
		}
		return null;
	}

	private Text getEndingMessage(ServerPlayerEntity winner) {
		if (winner != null) {
			return Text.translatable("text.electricfloor.win", winner.getDisplayName()).formatted(Formatting.GOLD);
		}
		return Text.translatable("text.electricfloor.no_winners").formatted(Formatting.GOLD);
	}

	private Vec3d getSpectatorSpawnPos() {
		Vec3d center = this.map.getPlatform().center();
		return new Vec3d(center.getX(), 4, center.getZ());
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	public PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.getSpectatorSpawnPos()).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	public void removePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	public void eliminate(ServerPlayerEntity eliminatedPlayer, boolean remove) {
		if (this.isGameEnding()) return;
		if (!this.players.contains(eliminatedPlayer)) return;

		Text message = Text.translatable("text.electricfloor.eliminated", eliminatedPlayer.getDisplayName()).formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer);

		this.applyPlayerFinishStatistics(eliminatedPlayer, StatisticKeys.GAMES_LOST);
	}

	public void applyPlayerFinishStatistics(ServerPlayerEntity player, StatisticKey<Integer> finishTypeKey) {
		if (!this.singleplayer && this.statistics != null) {
			this.statistics.forPlayer(player).increment(finishTypeKey, 1);
			this.statistics.forPlayer(player).set(StatisticKeys.LONGEST_TIME, this.timeElapsed);
		}
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.eliminate(player, true);
		return ActionResult.SUCCESS;
	}

	private void endGame() {
		this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}
}
