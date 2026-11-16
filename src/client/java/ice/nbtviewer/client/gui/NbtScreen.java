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
    private final String[] lines;
    private final int[] lineOffsets;
    private int scroll;
    private boolean draggingScrollbar, selecting;
    private double scrollbarDragOffset;
    private int selectionStart = -1, selectionEnd = -1;

    public NbtScreen(Component title, CompoundTag nbt) {
        super(title);
        compact = nbt.toString();
        formatted = pretty(compact);
        lines = formatted.split("\\n", -1);
        lineOffsets = new int[lines.length];
        int offset = 0;
        for (int i = 0; i < lines.length; i++) {
            lineOffsets[i] = offset;
            offset += lines[i].length() + 1;
        }
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

    private int maximumScroll() {
        return Math.max(0, lines.length * font.lineHeight - panelHeight() + 16);
    }

    private String selectedText() {
        int from = Math.min(selectionStart, selectionEnd);
        int to = Math.max(selectionStart, selectionEnd);
        if (from >= 0 && to > from) {
            from = Math.min(from, formatted.length());
            to = Math.min(to, formatted.length());
            return formatted.substring(from, to);
        }
        return "";
    }

    private int characterAt(double mouseX, double mouseY, boolean clamp) {
        if (lines.length == 0) return 0;
        int panelWidth = Math.min(760, width - 40), left = (width - panelWidth) / 2;
        int line = (int) Math.floor((mouseY - 35 - 8 + scroll) / font.lineHeight);
        if (clamp) {
            if (line < 0) return 0;
            if (line >= lines.length) return formatted.length();
        } else {
            if (line < 0) {
                if (mouseY >= 35 && scroll == 0) {
                    line = 0;
                } else {
                    return -1;
                }
            } else if (line >= lines.length) {
                return formatted.length();
            }
        }

        String value = lines[line];
        int x = (int) mouseX - left - 12;
        int index = 0;
        if (x > 0) {
            while (index < value.length() && font.width(value.substring(0, index + 1)) <= x) {
                index++;
            }
        }

        return lineOffsets[line] + index;
    }

    private int findWordStart(int pos) {
        if (formatted.isEmpty()) return 0;
        int i = Math.min(pos, formatted.length() - 1);
        if (i < 0) return 0;
        char c = formatted.charAt(i);
        if (c == '\n') return pos;
        boolean wordChar = isWordChar(c);
        while (i > 0 && isWordChar(formatted.charAt(i - 1)) == wordChar && formatted.charAt(i - 1) != '\n') {
            i--;
        }
        return i;
    }

    private int findWordEnd(int pos) {
        if (formatted.isEmpty()) return 0;
        int i = Math.min(pos, formatted.length() - 1);
        if (i < 0) return 0;
        char c = formatted.charAt(i);
        if (c == '\n') return pos;
        boolean wordChar = isWordChar(c);
        while (i < formatted.length() && isWordChar(formatted.charAt(i)) == wordChar && formatted.charAt(i) != '\n') {
            i++;
        }
        return i;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.' || c == '-';
    }

    private void updateScrollbar(double mouseY) {
        int top = 35;
        int height = panelHeight();
        int thumb = scrollbarThumbHeight();
        int travel = height - thumb;
        double fraction = travel <= 0
            ? 0
            : Math.max(0, Math.min(1, (mouseY - top - scrollbarDragOffset) / travel));
        scroll = (int) (maximumScroll() * fraction);
    }

    private int scrollbarThumbTop() {
        int thumb = scrollbarThumbHeight();
        return 35 + (maximumScroll() == 0
            ? 0
            : (panelHeight() - thumb) * scroll / maximumScroll());
    }

    private int scrollbarThumbHeight() {
        int content = lines.length * font.lineHeight;
        return Math.max(24, panelHeight() * panelHeight() / Math.max(panelHeight(), content));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(760, width - 40);
        int panelHeight = Math.min(480, height - 90);
        int left = (width - panelWidth) / 2;
        int top = 35;

        addRenderableWidget(Button.builder(Component.translatable("gui.nbt-viewer.copy"), button -> {
            if (selectionStart >= 0 && selectionStart != selectionEnd) {
                minecraft.keyboardHandler.setClipboard(selectedText());
            } else {
                minecraft.keyboardHandler.setClipboard(compact);
            }
        }).bounds(left, top + panelHeight + 8, 90, 20).build());

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
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int offset = lineOffsets[i];
            if (lineY + font.lineHeight >= top && lineY <= top + panelHeight) {
                if (selectionStart >= 0 && selectionStart != selectionEnd) {
                    int start = Math.min(selectionStart, selectionEnd);
                    int end = Math.max(selectionStart, selectionEnd);
                    int from = Math.max(start, offset) - offset;
                    int to = Math.min(end, offset + line.length()) - offset;
                    if (from < to) {
                        int x1 = left + 12 + font.width(line.substring(0, from));
                        int x2 = left + 12 + font.width(line.substring(0, to));
                        if (end > offset + line.length()) {
                            x2 += 4;
                        }
                        graphics.fill(x1, lineY - 1, x2, lineY + font.lineHeight, 0xFF406080);
                    }
                }
                graphics.text(font, line, left + 12, lineY, 0xFFE0E0E0, false);
            }
            lineY += font.lineHeight;
        }

        graphics.disableScissor();
        int thumb = scrollbarThumbHeight();
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

        if (button == InputConstants.MOUSE_BUTTON_LEFT && insideScrollbar) {
            draggingScrollbar = true;
            if (y >= thumbTop && y <= thumbBottom) {
                scrollbarDragOffset = y - thumbTop;
            } else {
                scrollbarDragOffset = scrollbarThumbHeight() / 2.0;
                updateScrollbar(y);
            }
            setDragging(true);
            return true;
        }

        if (button == InputConstants.MOUSE_BUTTON_LEFT && insidePanel) {
            int position = characterAt(x, y, false);
            if (position >= 0) {
                if (doubleClick) {
                    selectionStart = findWordStart(position);
                    selectionEnd = findWordEnd(position);
                    selecting = false;
                } else {
                    selecting = true;
                    selectionStart = position;
                    selectionEnd = position;
                }
                setDragging(true);
                return true;
            }
        }

        if (button == InputConstants.MOUSE_BUTTON_RIGHT && selectionStart >= 0 && selectionStart != selectionEnd) {
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
            if (y < 35) {
                scroll = Math.max(0, scroll - font.lineHeight);
            } else if (y > 35 + panelHeight()) {
                scroll = Math.min(maximumScroll(), scroll + font.lineHeight);
            }
            int position = characterAt(x, y, true);
            if (position >= 0) selectionEnd = position;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            draggingScrollbar = false;
            selecting = false;
            setDragging(false);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isCopy() || ((event.hasControlDown() || event.hasControlDownWithQuirk()) && event.key() == InputConstants.KEY_C)) {
            if (selectionStart >= 0 && selectionStart != selectionEnd) {
                minecraft.keyboardHandler.setClipboard(selectedText());
            } else {
                minecraft.keyboardHandler.setClipboard(compact);
            }
            return true;
        }
        if (event.isSelectAll() || ((event.hasControlDown() || event.hasControlDownWithQuirk()) && event.key() == InputConstants.KEY_A)) {
            selectionStart = 0;
            selectionEnd = formatted.length();
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