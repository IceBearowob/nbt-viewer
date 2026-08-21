package ice.nbtviewer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import org.lwjgl.glfw.GLFW;

import ice.nbtviewer.client.gui.NbtScreen;
import ice.nbtviewer.client.util.NbtCapture;

public class NbtViewerClient implements ClientModInitializer {
	public static KeyMapping showNbt;

	@Override
	public void onInitializeClient() {
		showNbt = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.nbt-viewer.show_nbt",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_N,
			KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (showNbt.consumeClick() && client.gui.screen() == null) {
				NbtCapture.captureLookingAt(client).ifPresent(nbt ->
					client.setScreenAndShow(new NbtScreen(
						Component.translatable("screen.nbt-viewer.entity"), nbt
					))
				);
			}
		});

		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (NbtViewerConfig.showItemTooltip()) {
				lines.add(Component.translatable(
					"tooltip.nbt-viewer.show_nbt",
					showNbt.getTranslatedKeyMessage()
				));
			}
		});
	}
}
