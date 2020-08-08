package io.github.haykam821.electricfloor.game.phase;

import java.util.Iterator;
import java.util.Set;

import io.github.haykam821.electricfloor.Main;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.gegy1000.plasmid.game.Game;
import net.gegy1000.plasmid.game.event.GameOpenListener;
import net.gegy1000.plasmid.game.event.GameTickListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.PlayerRejoinListener;
import net.gegy1000.plasmid.game.map.GameMap;
import net.gegy1000.plasmid.game.rule.GameRule;
import net.gegy1000.plasmid.game.rule.RuleResult;
import net.gegy1000.plasmid.util.PlayerRef;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class ElectricFloorActivePhase {
	private final ElectricFloorConfig config;
	private final Set<PlayerRef> players;
	private boolean singleplayer;
	private final Long2IntMap convertPositions = new Long2IntOpenHashMap();

	public ElectricFloorActivePhase(ElectricFloorConfig config, Set<PlayerRef> players) {
		this.config = config;
		this.players = players;
	}

	public static void setRules(Game.Builder builder) {
		builder.setRule(GameRule.ALLOW_CRAFTING, RuleResult.DENY);
		builder.setRule(GameRule.ALLOW_PORTALS, RuleResult.DENY);
		builder.setRule(GameRule.ALLOW_PVP, RuleResult.DENY);
		builder.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		builder.setRule(GameRule.ENABLE_HUNGER, RuleResult.DENY);
	}

	public static Game open(GameMap map, ElectricFloorConfig config, Set<PlayerRef> players) {
		ElectricFloorActivePhase game = new ElectricFloorActivePhase(config, players);

		Game.Builder builder = Game.builder();
		builder.setMap(map);

		ElectricFloorActivePhase.setRules(builder);

		// Listeners
		builder.on(GameOpenListener.EVENT, game::open);
		builder.on(GameTickListener.EVENT, game::tick);
		builder.on(PlayerAddListener.EVENT, game::addPlayer);
		builder.on(PlayerDeathListener.EVENT, game::onPlayerDeath);
		builder.on(PlayerRejoinListener.EVENT, game::rejoinPlayer);

		return builder.build();
	}

	public void open(Game game) {
		this.singleplayer = this.players.size() == 1;
 		for (PlayerRef playerRef : this.players) {
			playerRef.ifOnline(game.getWorld(), player -> {
				player.setGameMode(GameMode.ADVENTURE);
				ElectricFloorActivePhase.spawn(game.getMap(), player);
			});
		}
	}

	public void tick(Game game) {
		BlockPos.Mutable pos = new BlockPos.Mutable();

 		ObjectIterator<Long2IntMap.Entry> iterator = Long2IntMaps.fastIterator(this.convertPositions);
		while (iterator.hasNext()) {
			Long2IntMap.Entry entry = iterator.next();
			long convertPos = entry.getLongKey();
			int ticksLeft = entry.getIntValue();

			if (ticksLeft == 0) {
				pos.set(convertPos);
		
				BlockState state = game.getWorld().getBlockState(pos);
				game.getWorld().setBlockState(pos, Main.getConvertedFloor(state));

				iterator.remove();
			} else {
				entry.setValue(ticksLeft - 1);
			}
		}

		Iterator<PlayerRef> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			PlayerRef playerRef = playerIterator.next();
			playerRef.ifOnline(game.getWorld(), player -> {
				BlockPos landingPos = player.getLandingPos();
				BlockState state = game.getWorld().getBlockState(landingPos);

				if (Main.isConvertible(state)) {
					BlockState convertedState = Main.getConvertedFloor(state);
					if (convertedState == null) {
						this.eliminate(game, player, false);
						playerIterator.remove();
					} else {
						this.convertPositions.putIfAbsent(landingPos.asLong(), this.config.getDelay());
					}
				}
			});
		}

		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			Text endingMessage = this.getEndingMessage(game);
			game.onlinePlayers().forEach(player -> {
				player.sendMessage(endingMessage, false);
			});

			game.close();
		}
	}

	private Text getEndingMessage(Game game) {
		if (this.players.size() == 1) {
			PlayerRef winnerRef = this.players.iterator().next();
			if (winnerRef.isOnline(game.getWorld())) {
				PlayerEntity winner = winnerRef.getEntity(game.getWorld());
				return winner.getDisplayName().shallowCopy().append(" has won the game!").formatted(Formatting.GOLD);
			}
		}
		return new LiteralText("Nobody won the game!").formatted(Formatting.GOLD);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	public void addPlayer(Game game, PlayerEntity player) {
		if (!this.players.contains(PlayerRef.of(player))) {
			this.setSpectator(player);
		}
	}

	public void eliminate(Game game, PlayerEntity eliminatedPlayer, boolean remove) {
		Text message = eliminatedPlayer.getDisplayName().shallowCopy().append(" has been eliminated!").formatted(Formatting.RED);
		game.onlinePlayers().forEach(player -> {
			player.sendMessage(message, false);
		});

		if (remove) {
			this.players.remove(PlayerRef.of(eliminatedPlayer));
		}
		this.setSpectator(eliminatedPlayer);
	}

	public boolean onPlayerDeath(Game game, PlayerEntity player, DamageSource source) {
		this.eliminate(game, player, true);
		return true;
	}

	public void rejoinPlayer(Game game, PlayerEntity player) {
		this.eliminate(game, player, true);
	}

	public static void spawn(GameMap map, ServerPlayerEntity player) {
		Vec3d center = map.getFirstRegion("platform").getCenter();
		player.teleport(map.getWorld(), center.getX(), center.getY() + 0.5, center.getZ(), 0, 0);
	}
}