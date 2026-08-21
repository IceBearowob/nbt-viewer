package ice.nbtviewer.client.util;

import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.EntityHitResult;

/** Client-side snapshotting only; no packets are sent to the server. */
public final class NbtCapture {
	private NbtCapture() {}

	public static Optional<NbtCompound> captureLookingAt(MinecraftClient client) {
		if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() != null) {
			Entity entity = hit.getEntity();
			NbtCompound result = new NbtCompound();
			entity.writeNbt(result);
			return Optional.of(result);
		}
		return Optional.empty();
	}
}
