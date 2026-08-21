package ice.nbtviewer.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class NbtScreen extends Screen {
 private final String formatted; private final String compact; private int scroll;
 public NbtScreen(Component title, CompoundTag nbt) { super(title); compact = nbt.toString(); formatted = pretty(compact); }
 private static String pretty(String source) {
  StringBuilder out = new StringBuilder(); int depth = 0; boolean quoted = false; boolean escaped = false;
  for (int i = 0; i < source.length(); i++) {
   char ch = source.charAt(i);
   if (escaped) { out.append(ch); escaped = false; continue; }
   if (ch == '\\' && quoted) { out.append(ch); escaped = true; continue; }
   if (ch == '"') { quoted = !quoted; out.append(ch); continue; }
   if (quoted) { out.append(ch); continue; }
   if (ch == '{' || ch == '[') { out.append(ch).append('\n'); depth++; indent(out, depth); }
   else if (ch == '}' || ch == ']') { out.append('\n'); depth = Math.max(0, depth - 1); indent(out, depth); out.append(ch); }
   else if (ch == ',') { out.append(ch).append('\n'); indent(out, depth); }
   else if (ch == ':') { out.append(':'); }
   else if (ch == ' ' && (out.length() == 0 || out.charAt(out.length() - 1) == '\n')) { }
   else { out.append(ch); }
  }
  return out.toString();
 }
 private static void indent(StringBuilder out, int depth) { out.append("  ".repeat(depth)); }
 @Override protected void init() { int w=Math.min(760,width-40), h=Math.min(480,height-90), x=(width-w)/2,y=35; addRenderableWidget(Button.builder(Component.translatable("gui.nbt-viewer.copy"), b -> minecraft.keyboardHandler.setClipboard(compact)).bounds(x,y+h+8,90,20).build()); addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x+w-90,y+h+8,90,20).build()); }
 @Override public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float delta) {
  c.fill(0,0,width,height,0xFF202020);
  int w=Math.min(760,width-40),h=Math.min(480,height-90),x=(width-w)/2,y=35;
  c.text(font,title,width/2-font.width(title)/2,12,0xFFFFFFFF);
  c.fill(x,y,x+w,y+h,0xCC101018);
  c.enableScissor(x,y,x+w,y+h);
  int lineY=y+6-scroll;
  for(String line:formatted.split("\\n",-1)){
   c.text(font,Component.literal(line),x+8,lineY,0xFFE0E0E0);
   lineY+=font.lineHeight;
  }
  c.disableScissor();
  super.extractRenderState(c,mx,my,delta);
 }
 @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) { int max=Math.max(0,formatted.split("\\n",-1).length*font.lineHeight-(Math.min(480,height-90)-12)); scroll=(int)Math.max(0,Math.min(max,scroll-(vertical*font.lineHeight))); return true; }
}
