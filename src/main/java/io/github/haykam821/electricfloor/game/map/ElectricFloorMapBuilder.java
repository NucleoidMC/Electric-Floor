package io.github.haykam821.electricfloor.game.map;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import net.gegy1000.plasmid.game.map.template.MapTemplate;
import net.gegy1000.plasmid.util.BlockBounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

public class ElectricFloorMapBuilder {
	private final ElectricFloorConfig config;

	public ElectricFloorMapBuilder(ElectricFloorConfig config) {
		this.config = config;
	}

	public CompletableFuture<ElectricFloorMap> create() {
		return CompletableFuture.supplyAsync(() -> {
			MapTemplate template = MapTemplate.createEmpty();
			ElectricFloorMapConfig mapConfig = this.config.getMapConfig();

			BlockBounds bounds = new BlockBounds(BlockPos.ORIGIN, new BlockPos(mapConfig.x - 1, 0, mapConfig.z - 1));
			this.build(bounds, template);

			return new ElectricFloorMap(template, bounds);
		}, Util.getServerWorkerExecutor());
	}

	public void build(BlockBounds bounds, MapTemplate template) {
		BlockState state = Blocks.WHITE_STAINED_GLASS.getDefaultState();
		for (BlockPos pos : bounds.iterate()) {
			template.setBlockState(pos, state);
		}
	}
}