package io.github.haykam821.electricfloor.game.phase;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ElectricFloorActivePhase {
	private final ServerWorld world;
	private final GameWorld gameWorld;
	private final ElectricFloorMap map;
	private final ElectricFloorConfig config;
	private final Set<ServerPlayerEntity> players = new HashSet<>();
	private boolean singleplayer;
	private final Long2IntMap convertPositions = new Long2IntOpenHashMap();
	private boolean opened;

	public ElectricFloorActivePhase(GameWorld gameWorld, ElectricFloorMap map, ElectricFloorConfig config) {
		this.world = gameWorld.getWorld();
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static void setRules(Game game) {
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
	}

	public static void open(GameWorld gameWorld, ElectricFloorMap map, ElectricFloorConfig config) {
		ElectricFloorActivePhase phase = new ElectricFloorActivePhase(gameWorld, map, config);
		phase.players.addAll(gameWorld.getPlayers());

		gameWorld.openGame(game -> {
			ElectricFloorActivePhase.setRules(game);

			// Listeners
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerRemoveListener.EVENT, phase::removePlayer);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
		});
	}

	public void open() {
		this.opened = true;
		this.singleplayer = this.players.size() == 1;

		ElectricFloorMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.x, mapConfig.z) - 3) / 2;

		Vec3d center = this.map.getPlatform().getCenter();

		int i = 0;
 		for (ServerPlayerEntity player : this.players) {
			player.setGameMode(GameMode.ADVENTURE);

			double theta = ((double) i++ / this.players.size()) * 2 * Math.PI;
			double x = center.getX() + Math.sin(theta) * spawnRadius;
			double z = center.getZ() + Math.cos(theta) * spawnRadius;
			double y = center.getY() + 0.5;

			player.teleport(this.gameWorld.getWorld(), x, y, z, 0, 0);
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

		Iterator<ServerPlayerEntity> playerIterator = this.players.iterator();
		while (playerIterator.hasNext()) {
			ServerPlayerEntity player = playerIterator.next();
			if (!this.map.getBox().contains(player.getPos())) {
				this.eliminate(player, false);
				playerIterator.remove();
			}

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
		}

		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			Text endingMessage = this.getEndingMessage();
			for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.gameWorld.close();
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			ServerPlayerEntity winner = this.players.iterator().next();
			return winner.getDisplayName().shallowCopy().append(" has won the game!").formatted(Formatting.GOLD);
		}
		return new LiteralText("Nobody won the game!").formatted(Formatting.GOLD);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	public void addPlayer(ServerPlayerEntity player) {
		if (!this.players.contains(player)) {
			this.setSpectator(player);
		} else if (this.opened) {
			this.eliminate(player, true);
		}
	}

	public void removePlayer(ServerPlayerEntity player) {
		this.eliminate(player, true);
	}

	public void eliminate(ServerPlayerEntity eliminatedPlayer, boolean remove) {
		Text message = eliminatedPlayer.getDisplayName().shallowCopy().append(" has been eliminated!").formatted(Formatting.RED);
		for (ServerPlayerEntity player : this.gameWorld.getPlayers()) {
			player.sendMessage(message, false);
		}

		if (remove) {
			this.players.remove(eliminatedPlayer);
		}
		this.setSpectator(eliminatedPlayer);
	}

	public ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.eliminate(player, true);
		return ActionResult.SUCCESS;
	}
}
