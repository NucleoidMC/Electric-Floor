package io.github.haykam821.electricfloor;

import java.util.HashMap;
import java.util.Map;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.map.ElectricFloorMapProvider;
import io.github.haykam821.electricfloor.game.phase.ElectricFloorWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.gegy1000.plasmid.game.GameType;
import net.gegy1000.plasmid.game.config.GameMapConfig;
import net.gegy1000.plasmid.game.map.provider.MapProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class Main implements ModInitializer {
	public static final String MOD_ID = "electricfloor";

	public static final Map<Block, Block> FLOOR_CONVERSIONS = new HashMap<>();

	private static final Identifier ELECTRIC_FLOOR_ID = new Identifier(MOD_ID, "electric_floor");
	public static final GameType<ElectricFloorConfig> ELECTRIC_FLOOR_TYPE = GameType.register(ELECTRIC_FLOOR_ID, (server, config) -> {
		GameMapConfig<ElectricFloorConfig> mapConfig = config.getMapConfig();

		RegistryKey<World> dimension = mapConfig.getDimension();
		BlockPos origin = mapConfig.getOrigin();
		ServerWorld world = server.getWorld(dimension);

		return mapConfig.getProvider().createAt(world, origin, config).thenApply(map -> {
			return ElectricFloorWaitingPhase.open(map, config);
		});
	}, ElectricFloorConfig.CODEC);

	@Override
	public void onInitialize() {
		MapProvider.REGISTRY.register(ELECTRIC_FLOOR_ID, ElectricFloorMapProvider.CODEC);
	}

	public static BlockState getConvertedFloor(BlockState state) {
		Block block = FLOOR_CONVERSIONS.get(state.getBlock());
		return block == null ? null : block.getDefaultState();
	}

	public static boolean isConvertible(BlockState state) {
		return FLOOR_CONVERSIONS.containsKey(state.getBlock());
	}

	static {
		FLOOR_CONVERSIONS.put(Blocks.WHITE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.MAGENTA_STAINED_GLASS, Blocks.RED_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.RED_STAINED_GLASS, null);
	}
}