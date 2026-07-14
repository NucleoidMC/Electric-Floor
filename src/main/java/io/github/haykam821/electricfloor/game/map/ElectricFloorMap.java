package io.github.haykam821.electricfloor.game.map;

import java.util.Set;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

public class ElectricFloorMap {
	private final MapTemplate template;
	private final BlockBounds platform;
	private final AABB box;

	public ElectricFloorMap(MapTemplate template, BlockBounds platform) {
		this.template = template;
		this.platform = platform;
		this.box = this.platform.asBox().inflate(-1, -0.5, -1);
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public AABB getBox() {
		return this.box;
	}

	public Vec3 getGuideTextPos() {
		return this.createCenterPos(2.2, 0);
	}

	public Vec3 getWaitingSpawnPos() {
		return this.createCenterPos(1, 4);
	}

	public void teleportToWaitingSpawn(ServerPlayer player, ServerLevel world) {
		Vec3 pos = this.getWaitingSpawnPos();
		player.teleportTo(world, pos.x(), pos.y(), pos.z(), Set.of(), 0, 0, true);
	}

	public Vec3 getSpectatorSpawnPos() {
		return this.createCenterPos(4, 0);
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private Vec3 createCenterPos(double y, double offsetZ) {
		Vec3 center = this.getPlatform().center();

		double maxOffsetZ = this.platform.size().getZ() / 2 - 0.5;
		double clampedOffsetZ = Mth.clamp(offsetZ, -maxOffsetZ, maxOffsetZ);

		return new Vec3(center.x(), y, center.z() - clampedOffsetZ);
	}
}
