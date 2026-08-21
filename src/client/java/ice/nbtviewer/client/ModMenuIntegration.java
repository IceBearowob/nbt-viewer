package ice.nbtviewer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import net.minecraft.network.chat.Component;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("config.nbt-viewer.title"))
            .category(createGeneralCategory())
            .save(() -> {})
            .build()
            .generateScreen(parent);
    }

    private static ConfigCategory createGeneralCategory() {
        var enabled = Option.<Boolean>createBuilder()
            .name(Component.translatable("config.nbt-viewer.enabled"))
            .description(OptionDescription.of(Component.translatable("config.nbt-viewer.enabled.description")))
            .binding(true, NbtViewerConfig::enabled, NbtViewerConfig::setEnabled)
            .controller(BooleanControllerBuilder::create)
            .build();

        var showTooltip = Option.<Boolean>createBuilder()
            .name(Component.translatable("config.nbt-viewer.show_tooltip"))
            .description(OptionDescription.of(
                Component.translatable("config.nbt-viewer.show_tooltip.description")))
            .binding(true, NbtViewerConfig::showItemTooltip, NbtViewerConfig::setShowItemTooltip)
            .controller(BooleanControllerBuilder::create)
            .build();

        return ConfigCategory.createBuilder()
            .name(Component.translatable("config.nbt-viewer.category.general"))
            .option(enabled)
            .option(showTooltip)
            .build();
    }
}
