package ice.nbtviewer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import org.lwjgl.glfw.GLFW;

import ice.nbtviewer.client.gui.NbtScreen;
import ice.nbtviewer.client.util.NbtCapture;

public class NbtViewerClient implements ClientModInitializer {
	public static KeyBinding showNbt;

	@Override
	public void onInitializeClient() {
		showNbt = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.nbt-viewer.show_nbt", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N,
			"category.nbt-viewer"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (showNbt.wasPressed() && client.currentScreen == null) {
				NbtCapture.captureLookingAt(client).ifPresent(nbt ->
					client.setScreen(new NbtScreen(Text.translatable("screen.nbt-viewer.entity"), nbt)));
			}
		});
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> lines.add(
			Text.translatable("tooltip.nbt-viewer.show_nbt", showNbt.getBoundKeyLocalizedText())));
	}
}
