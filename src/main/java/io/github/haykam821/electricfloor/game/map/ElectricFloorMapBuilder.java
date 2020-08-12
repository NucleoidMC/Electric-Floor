package io.github.haykam821.electricfloor.game.map;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class ElectricFloorMapBuilder {
	private static final BlockState FLOOR = Blocks.WHITE_STAINED_GLASS.getDefaultState();
	private static final BlockState FLOOR_OUTLINE = Blocks.SMOOTH_STONE.getDefaultState();
	private static final BlockState WALL = Blocks.STONE_BRICK_WALL.getDefaultState();
	private static final BlockState WALL_TOP = Blocks.SMOOTH_STONE_SLAB.getDefaultState();

	private final ElectricFloorConfig config;

	public ElectricFloorMapBuilder(ElectricFloorConfig config) {
		this.config = config;
	}

	public CompletableFuture<ElectricFloorMap> create() {
		return CompletableFuture.supplyAsync(() -> {
			MapTemplate template = MapTemplate.createEmpty();
			ElectricFloorMapConfig mapConfig = this.config.getMapConfig();

			BlockBounds bounds = new BlockBounds(BlockPos.ORIGIN, new BlockPos(mapConfig.x + 1, 2, mapConfig.z + 1));
			this.build(bounds, template, mapConfig);

			return new ElectricFloorMap(template, bounds);
		}, Util.getMainWorkerExecutor());
	}

	private BlockState getBlockState(BlockPos pos, BlockBounds bounds, ElectricFloorMapConfig mapConfig) {
		int layer = pos.getY() - bounds.getMin().getY();
		boolean outline = pos.getX() == bounds.getMin().getX() || pos.getX() == bounds.getMax().getX() || pos.getZ() == bounds.getMin().getZ() || pos.getZ() == bounds.getMax().getZ();

		if (outline) {
			if (layer == 0) {
				return FLOOR_OUTLINE;
			} else if (layer == 1) {
				return WALL;
			} else if (layer == 2) {
				return WALL_TOP;
			}
		} else if (layer == 0) {
			return FLOOR;
		}

		return null;
	}

	public void build(BlockBounds bounds, MapTemplate template, ElectricFloorMapConfig mapConfig) {
		for (BlockPos pos : bounds.iterate()) {
			BlockState state = this.getBlockState(pos, bounds, mapConfig);
			if (state != null) {
				template.setBlockState(pos, state);
			}
		}
	}
}