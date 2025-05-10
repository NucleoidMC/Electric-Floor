package io.github.haykam821.electricfloor;

import java.util.HashMap;
import java.util.Map;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import io.github.haykam821.electricfloor.game.phase.ElectricFloorWaitingPhase;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;

public class Main implements ModInitializer {
	private static final String MOD_ID = "electricfloor";

	public static final Block SPAWN_PLATFORM = Blocks.RED_TERRACOTTA;
	public static final Map<Block, Block> FLOOR_CONVERSIONS = new HashMap<>();
	public static final Object2IntMap<Block> FLOOR_LIGHT = new Object2IntOpenHashMap<>();

	private static final Identifier ELECTRIC_FLOOR_ID = Main.identifier("electric_floor");
	public static final GameType<ElectricFloorConfig> ELECTRIC_FLOOR_TYPE = GameType.register(ELECTRIC_FLOOR_ID, ElectricFloorConfig.CODEC, ElectricFloorWaitingPhase::open);

	private static final Identifier BLOCKS_CONVERTED_ID = Main.identifier("blocks_converted");
	public static final StatisticKey<Integer> BLOCKS_CONVERTED = StatisticKey.intKey(BLOCKS_CONVERTED_ID);

	@Override
	public void onInitialize() {
		return;
	}

	public static BlockState getConvertedFloor(BlockState state) {
		Block block = FLOOR_CONVERSIONS.get(state.getBlock());
		return block == null ? null : block.getDefaultState();
	}

	public static boolean isConvertible(BlockState state) {
		if (state.getBlock() == SPAWN_PLATFORM) return false;
		return FLOOR_CONVERSIONS.containsKey(state.getBlock());
	}

	public static BlockState getFloorLightState(BlockState state, boolean night) {
		if (night) {
			int light = FLOOR_LIGHT.getInt(state.getBlock());

			if (light > 0) {
				return Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, light);
			}
		}

		return null;
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}

	static {
		FLOOR_CONVERSIONS.put(SPAWN_PLATFORM, Blocks.WHITE_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.WHITE_STAINED_GLASS, Blocks.LIGHT_BLUE_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.MAGENTA_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.MAGENTA_STAINED_GLASS, Blocks.RED_STAINED_GLASS);
		FLOOR_CONVERSIONS.put(Blocks.RED_STAINED_GLASS, null);

		FLOOR_LIGHT.put(Blocks.LIGHT_BLUE_STAINED_GLASS, 5);
		FLOOR_LIGHT.put(Blocks.MAGENTA_STAINED_GLASS, 10);
		FLOOR_LIGHT.put(Blocks.RED_STAINED_GLASS, 15);
	}
}