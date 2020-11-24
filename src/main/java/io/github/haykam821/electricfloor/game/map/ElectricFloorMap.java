package io.github.haykam821.electricfloor.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Box;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class ElectricFloorMap {
	private final MapTemplate template;
	private final BlockBounds platform;
	private final Box box;

	public ElectricFloorMap(MapTemplate template, BlockBounds platform) {
		this.template = template;
		this.platform = platform;
		this.box = this.platform.toBox().expand(-1, -0.5, -1);
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public Box getBox() {
		return this.box;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}
