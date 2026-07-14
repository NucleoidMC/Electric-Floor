package io.github.haykam821.electricfloor.game.map;

import io.github.haykam821.electricfloor.Main;
import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;

public class ElectricFloorMapBuilder {
	private static final BlockState FLOOR = Blocks.STAINED_GLASS.white().defaultBlockState();
	private static final BlockState FLOOR_OUTLINE = Blocks.SMOOTH_STONE.defaultBlockState();
	private static final BlockState WALL = Blocks.STONE_BRICK_WALL.defaultBlockState();
	private static final BlockState WALL_TOP = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();

	private final ElectricFloorConfig config;

	public ElectricFloorMapBuilder(ElectricFloorConfig config) {
		this.config = config;
	}

	public ElectricFloorMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		ElectricFloorMapConfig mapConfig = this.config.getMapConfig();

		BlockBounds bounds = BlockBounds.of(BlockPos.ZERO, new BlockPos(mapConfig.x + 1, 2, mapConfig.z + 1));
		this.build(bounds, template, mapConfig);

		return new ElectricFloorMap(template, bounds);
	}

	private BlockState getBlockState(BlockPos pos, BlockBounds bounds, ElectricFloorMapConfig mapConfig) {
		int layer = pos.getY() - bounds.min().getY();
		boolean outline = pos.getX() == bounds.min().getX() || pos.getX() == bounds.max().getX() || pos.getZ() == bounds.min().getZ() || pos.getZ() == bounds.max().getZ();

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
		BlockPos.MutableBlockPos upPos = new BlockPos.MutableBlockPos();

		for (BlockPos pos : bounds) {
			BlockState state = this.getBlockState(pos, bounds, mapConfig);
			if (state != null) {
				template.setBlockState(pos, state);

				BlockState lightState = Main.getFloorLightState(state, this.config.isNight());

				if (lightState != null) {
					upPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
					template.setBlockState(upPos, lightState);
				}
			}
		}
	}
}
