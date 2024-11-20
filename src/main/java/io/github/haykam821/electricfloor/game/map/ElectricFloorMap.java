package io.github.haykam821.electricfloor.game.map;

import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

public class ElectricFloorMap {
	private final MapTemplate template;
	private final BlockBounds platform;
	private final Box box;

	public ElectricFloorMap(MapTemplate template, BlockBounds platform) {
		this.template = template;
		this.platform = platform;
		this.box = this.platform.asBox().expand(-1, -0.5, -1);
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public Box getBox() {
		return this.box;
	}

	public Vec3d getGuideTextPos() {
		return this.createCenterPos(2.2, 0);
	}

	public Vec3d getWaitingSpawnPos() {
		return this.createCenterPos(1, 4);
	}

	public void teleportToWaitingSpawn(ServerPlayerEntity player, ServerWorld world) {
		Vec3d pos = this.getWaitingSpawnPos();
		player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), Set.of(), 0, 0, true);
	}

	public Vec3d getSpectatorSpawnPos() {
		return this.createCenterPos(4, 0);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private Vec3d createCenterPos(double y, double offsetZ) {
		Vec3d center = this.getPlatform().center();

		double maxOffsetZ = this.platform.size().getZ() / 2 - 0.5;
		double clampedOffsetZ = MathHelper.clamp(offsetZ, -maxOffsetZ, maxOffsetZ);

		return new Vec3d(center.getX(), y, center.getZ() - clampedOffsetZ);
	}
}
