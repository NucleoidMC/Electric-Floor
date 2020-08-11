package io.github.haykam821.electricfloor.game.phase;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.haykam821.electricfloor.Main;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.Game;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.PlayerRef;

public class ElectricFloorActivePhase {
	private final ServerWorld world;
	private final GameWorld gameWorld;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;
	private final Set<PlayerRef> players;
	private boolean singleplayer;
	private final Long2IntMap convertPositions = new Long2IntOpenHashMap();
	private boolean opened;

	public ElectricFloorActivePhase(GameWorld gameWorld, ElectricFloorMap map, ElectricFloorConfig config, Set<PlayerRef> players) {
		this.world = gameWorld.getWorld();
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
		this.players = players;
	}

	public static void setRules(Game game) {
		game.setRule(GameRule.ALLOW_CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.ALLOW_PORTALS, RuleResult.DENY);
		game.setRule(GameRule.ALLOW_PVP, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.ENABLE_HUNGER, RuleResult.DENY);
	}

	public static void open(GameWorld gameWorld, ElectricFloorMap map, ElectricFloorConfig config) {
		Set<PlayerRef> players = gameWorld.getPlayers().stream().map(PlayerRef::of).collect(Collectors.toSet());
		ElectricFloorActivePhase phase = new ElectricFloorActivePhase(gameWorld, map, config, players);

		gameWorld.newGame(game -> {
			ElectricFloorActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
		});
	}

	public void open() {
		this.opened = true;
		this.singleplayer = this.players.size() == 1;

 		for (PlayerRef playerRef : this.players) {
			playerRef.ifOnline(this.world, player -> {
				player.setGameMode(GameMode.ADVENTURE);
				ElectricFloorActivePhase.spawn(this.world, this.map, player);
			});
		}
	}

	public void tick() {
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

		Iterator<PlayerRef> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			PlayerRef playerRef = playerIterator.next();
			playerRef.ifOnline(this.world, player -> {
				BlockPos landingPos = player.getLandingPos();
				BlockState state = this.world.getBlockState(landingPos);

				if (Main.isConvertible(state)) {
					BlockState convertedState = Main.getConvertedFloor(state);
					if (convertedState == null) {
						this.eliminate(player, false);
						playerIterator.remove();
					} else {
						this.convertPositions.putIfAbsent(landingPos.asLong(), this.config.getDelay());
					}
				}
			});
		}

		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			Text endingMessage = this.getEndingMessage();
			for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.gameWorld.closeWorld();
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			PlayerRef winnerRef = this.players.iterator().next();
			if (winnerRef.isOnline(this.world)) {
				PlayerEntity winner = winnerRef.getEntity(this.world);
				return winner.getDisplayName().shallowCopy().append(" has won the game!").formatted(Formatting.GOLD);
			}
		}
		return new LiteralText("Nobody won the game!").formatted(Formatting.GOLD);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	public void addPlayer(PlayerEntity player) {
		if (!this.players.contains(PlayerRef.of(player))) {
			this.setSpectator(player);
		} else if (this.opened) {
			this.eliminate(player, true);
		}
	}

	public void eliminate(PlayerEntity eliminatedPlayer, boolean remove) {
		Text message = eliminatedPlayer.getDisplayName().shallowCopy().append(" has been eliminated!").formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(PlayerRef.of(eliminatedPlayer));
		}
		this.setSpectator(eliminatedPlayer);
	}

	public boolean onPlayerDeath(PlayerEntity player, DamageSource source) {
		this.eliminate(player, true);
		return true;
	}

	public void rejoinPlayer(PlayerEntity player) {
		this.eliminate(player, true);
	}

	public static void spawn(ServerWorld world, ElectricFloorMap map, ServerPlayerEntity player) {
		Vec3d center = map.getPlatform().getCenter();
		player.teleport(world, center.getX(), center.getY() + 0.5, center.getZ(), 0, 0);
	}
}