package ice.nbtviewer.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Screen used to display formatted NBT and copy its compact representation. */
public final class NbtScreen extends Screen {
    private final String formatted;
    private final String compact;
    private int scroll;

    public NbtScreen(Component title, CompoundTag nbt) {
        super(title);
        compact = nbt.toString();
        formatted = pretty(compact);
    }

    private static String pretty(String source) {
        StringBuilder result = new StringBuilder();
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;

        for (int index = 0; index < source.length(); index++) {
            char character = source.charAt(index);
            if (escaped) { result.append(character); escaped = false; continue; }
            if (character == '\\' && quoted) { result.append(character); escaped = true; continue; }
            if (character == '"') { quoted = !quoted; result.append(character); continue; }
            if (quoted) { result.append(character); continue; }

            switch (character) {
                case '{', '[' -> { result.append(character).append('\n'); indent(result, ++depth); }
                case '}', ']' -> { result.append('\n'); indent(result, Math.max(0, --depth)); result.append(character); }
                case ',' -> { result.append(',').append('\n'); indent(result, depth); }
                case ':' -> result.append(':');
                case ' ' -> { if (!result.isEmpty() && result.charAt(result.length() - 1) != '\n') result.append(character); }
                default -> result.append(character);
            }
        }
        return result.toString();
    }

    private static void indent(StringBuilder result, int depth) {
        result.append("  ".repeat(depth));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(760, width - 40);
        int panelHeight = Math.min(480, height - 90);
        int left = (width - panelWidth) / 2;
        int top = 35;

        addRenderableWidget(Button.builder(Component.translatable("gui.nbt-viewer.copy"),
                button -> minecraft.keyboardHandler.setClipboard(compact))
            .bounds(left, top + panelHeight + 8, 90, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(left + panelWidth - 90, top + panelHeight + 8, 90, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xFF202020);

        int panelWidth = Math.min(760, width - 40);
        int panelHeight = Math.min(480, height - 90);
        int left = (width - panelWidth) / 2;
        int top = 35;

        graphics.text(font, title, width / 2 - font.width(title) / 2, 12, 0xFFFFFFFF);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xCC101018);
        graphics.enableScissor(left, top, left + panelWidth, top + panelHeight);

        int lineY = top + 8 - scroll;
        for (String line : formatted.split("\\n", -1)) {
            graphics.text(font, line, left + 12, lineY, 0xFFE0E0E0, false);
            lineY += font.lineHeight;
        }

        graphics.disableScissor();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int panelHeight = Math.min(480, height - 90);
        int contentHeight = formatted.split("\\n", -1).length * font.lineHeight;
        int maximumScroll = Math.max(0, contentHeight - panelHeight + 16);
        scroll = (int) Math.max(0, Math.min(maximumScroll, scroll - vertical * font.lineHeight));
        return true;
    }
}
