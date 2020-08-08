package io.github.haykam821.electricfloor.game.map;

import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.electricfloor.game.ElectricFloorConfig;
import net.gegy1000.plasmid.game.map.GameMap;
import net.gegy1000.plasmid.game.map.GameMapBuilder;
import net.gegy1000.plasmid.game.map.provider.MapProvider;
import net.gegy1000.plasmid.world.BlockBounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ElectricFloorMapProvider implements MapProvider<ElectricFloorConfig> {
	public static final Codec<ElectricFloorMapProvider> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(map -> map.x),
			Codec.INT.fieldOf("z").forGetter(map -> map.z)
		).apply(instance, ElectricFloorMapProvider::new);
	});

	public final int x;
	public final int z;

	public ElectricFloorMapProvider(int x, int z) {
		this.x = x;
		this.z = z;
	}

	@Override
	public CompletableFuture<GameMap> createAt(ServerWorld world, BlockPos origin, ElectricFloorConfig config) {
		BlockBounds bounds = new BlockBounds(BlockPos.ORIGIN, new BlockPos(this.x - 1, 0, this.z - 1));

		GameMapBuilder builder = GameMapBuilder.open(world, origin, bounds);
		builder.addRegion("platform", bounds);

		return CompletableFuture.supplyAsync(() -> {
			this.build(bounds, builder, config);
			return builder.build();
		}, world.getServer());
	}

	public void build(BlockBounds bounds, GameMapBuilder builder, ElectricFloorConfig config) {
		BlockState state = Blocks.WHITE_STAINED_GLASS.getDefaultState();
		for (BlockPos pos : bounds.iterate()) {
			builder.setBlockState(pos, state);
		}
	}

	@Override
	public Codec<ElectricFloorMapProvider> getCodec() {
		return CODEC;
	}
}