package io.github.haykam821.electricfloor.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.game.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class ElectricFloorMap {
	private final MapTemplate template;
	private final BlockBounds platform;

	public ElectricFloorMap(MapTemplate template, BlockBounds platform) {
		this.template = template;
		this.platform = platform;
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template, BlockPos.ORIGIN);
	}
}