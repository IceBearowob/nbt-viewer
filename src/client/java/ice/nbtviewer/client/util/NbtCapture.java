package ice.nbtviewer.client.util;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.EntityHitResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;

/** Client-side snapshotting only; no packets are sent to the server. */
public final class NbtCapture {
	private NbtCapture() {}

	public static Optional<CompoundTag> captureLookingAt(Minecraft client) {
		if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() != null) {
			Entity entity = hit.getEntity();
			TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
			entity.saveWithoutId(output);
			return Optional.of(output.buildResult());
		}
		return Optional.empty();
	}

	public static CompoundTag captureItem(Minecraft client, ItemStack stack) {
		if (client.level == null) return new CompoundTag();
		DynamicOps<net.minecraft.nbt.Tag> ops = RegistryOps.create(NbtOps.INSTANCE, client.level.registryAccess());
		return ItemStack.CODEC.encodeStart(ops, stack).result()
			.filter(CompoundTag.class::isInstance).map(CompoundTag.class::cast)
			.orElseGet(CompoundTag::new);
	}
}
