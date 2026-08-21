package ice.nbtviewer.client.mixin;

import ice.nbtviewer.client.gui.NbtScreen;
import ice.nbtviewer.client.util.NbtCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow protected Slot hoveredSlot;
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void nbtViewer$showItemNbt(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() != 78 || hoveredSlot == null || hoveredSlot.getItem().isEmpty()) return;
        ItemStack stack = hoveredSlot.getItem();
        CompoundTag tag = NbtCapture.captureItem(Minecraft.getInstance(), stack);
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().gui.setScreen(
                new NbtScreen(stack.getHoverName(), tag)));
        cir.setReturnValue(true);
    }
}
