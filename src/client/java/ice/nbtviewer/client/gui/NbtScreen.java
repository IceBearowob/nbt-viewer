package ice.nbtviewer.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** Screen used to display formatted NBT and copy its compact representation. */
public final class NbtScreen extends Screen {
    private final String formatted;
    private final String compact;
    private int scroll;
    private boolean draggingScrollbar, selecting;
    private double scrollbarDragOffset;
    private int selectionStart = -1, selectionEnd = -1;

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

    private int panelHeight() {
        return Math.min(480, height - 90);
    }

    private String[] lines() {
        return formatted.split("\\n", -1);
    }

    private int maximumScroll() {
        return Math.max(0, lines().length * font.lineHeight - panelHeight() + 16);
    }
    private String selectedText() {
        int from = Math.min(selectionStart, selectionEnd);
        int to = Math.max(selectionStart, selectionEnd);
        return from >= 0 && to > from ? formatted.substring(from, to) : "";
    }
    private int characterAt(double mouseX, double mouseY) {
        int panelWidth = Math.min(760, width - 40), left = (width - panelWidth) / 2;
        int line = (int) ((mouseY - 35 - 8 + scroll) / font.lineHeight);
        String[] all = lines();
        if (line < 0 || line >= all.length) return -1;

        String value = all[line];
        int x = (int) mouseX - left - 12;
        int index = 0;
        while (index < value.length() && font.width(value.substring(0, index + 1)) < x) {
            index++;
        }

        int absolute = 0;
        for (int i = 0; i < line; i++) {
            absolute += all[i].length() + 1;
        }
        return absolute + index;
    }

    private void updateScrollbar(double mouseY) {
        int top = 35;
        int height = panelHeight();
        int content = lines().length * font.lineHeight;
        int thumb = Math.max(24, height * height / Math.max(height, content));
        int travel = height - thumb;
        double fraction = travel == 0
            ? 0
            : Math.max(0, Math.min(1, (mouseY - top - scrollbarDragOffset) / travel));
        scroll = (int) (maximumScroll() * fraction);
    }

    private int scrollbarThumbTop() {
        int content = lines().length * font.lineHeight;
        int thumb = Math.max(24, panelHeight() * panelHeight() / Math.max(panelHeight(), content));
        return 35 + (maximumScroll() == 0
            ? 0
            : (panelHeight() - thumb) * scroll / maximumScroll());
    }

    private int scrollbarThumbHeight() {
        int content = lines().length * font.lineHeight;
        return Math.max(24, panelHeight() * panelHeight() / Math.max(panelHeight(), content));
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
        int offset = 0;
        for (String line : lines()) {
            if (selectionStart >= 0 && selectionStart != selectionEnd) {
                int start = Math.min(selectionStart, selectionEnd), end = Math.max(selectionStart, selectionEnd);
                int from = Math.max(start, offset) - offset, to = Math.min(end, offset + line.length()) - offset;
                if (from < to) graphics.fill(left + 12 + font.width(line.substring(0, from)), lineY - 1, left + 12 + font.width(line.substring(0, to)), lineY + font.lineHeight, 0xFF406080);
            }
            graphics.text(font, line, left + 12, lineY, 0xFFE0E0E0, false);
            lineY += font.lineHeight;
            offset += line.length() + 1;
        }

        graphics.disableScissor();
        int content = lines().length * font.lineHeight, thumb = Math.max(24, panelHeight * panelHeight / Math.max(panelHeight, content));
        int thumbTop = scrollbarThumbTop();
        graphics.fill(left + panelWidth + 4, top, left + panelWidth + 8, top + panelHeight, 0xFF303038);
        graphics.fill(left + panelWidth + 3, thumbTop, left + panelWidth + 9, thumbTop + thumb, 0xFFAAAAAA);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();
        int button = event.button();
        int panelWidth = Math.min(760, width - 40);
        int left = (width - panelWidth) / 2;
        boolean insidePanel = x >= left && x <= left + panelWidth
            && y >= 35 && y <= 35 + panelHeight();
        boolean insideScrollbar = x >= left + panelWidth + 2
            && x <= left + panelWidth + 12 && y >= 35
            && y <= 35 + panelHeight();

        int thumbTop = scrollbarThumbTop();
        int thumbBottom = thumbTop + scrollbarThumbHeight();

        if (button == 0 && insideScrollbar && y >= thumbTop && y <= thumbBottom) {
            draggingScrollbar = true;
            scrollbarDragOffset = y - thumbTop;
            return true;
        }

        if (button == 0 && insidePanel) {
            int position = characterAt(x, y);
            if (position >= 0) {
                selecting = true;
                selectionStart = position;
                selectionEnd = position;
                return true;
            }
        }

        if (button == 1 && selectionStart >= 0 && selectionStart != selectionEnd) {
            minecraft.keyboardHandler.setClipboard(selectedText());
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double x = event.x();
        double y = event.y();
        if (draggingScrollbar) {
            updateScrollbar(y);
            return true;
        }
        if (selecting) {
            int position = characterAt(x, y);
            if (position >= 0) selectionEnd = position;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        selecting = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.modifiers() & 2) != 0
            && event.key() == InputConstants.KEY_C
            && selectionStart >= 0
            && selectionStart != selectionEnd) {
            minecraft.keyboardHandler.setClipboard(selectedText());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = (int) Math.max(0, Math.min(maximumScroll(), scroll - vertical * font.lineHeight));
        return true;
    }
}
