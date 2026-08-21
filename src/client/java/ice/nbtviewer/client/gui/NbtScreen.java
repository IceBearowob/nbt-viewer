package ice.nbtviewer.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class NbtScreen extends Screen {
 private final String formatted; private int scroll;
 public NbtScreen(Text title, NbtCompound nbt) { super(title); formatted = nbt.toString().replace(", ", ",\n").replace("{", "{\n").replace("}", "\n}"); }
 @Override protected void init() { int w=Math.min(760,width-40), h=Math.min(480,height-90), x=(width-w)/2,y=35; addDrawableChild(ButtonWidget.builder(Text.translatable("gui.nbt-viewer.copy"), b -> client.keyboard.setClipboard(formatted)).dimensions(x,y+h+8,90,20).build()); addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close()).dimensions(x+w-90,y+h+8,90,20).build()); }
 @Override public void render(DrawContext c,int mx,int my,float delta) { renderBackground(c,mx,my,delta); int w=Math.min(760,width-40),h=Math.min(480,height-90),x=(width-w)/2,y=35; c.drawCenteredTextWithShadow(textRenderer,title,width/2,12,0xFFFFFF); c.fill(x,y,x+w,y+h,0xCC101018); c.enableScissor(x,y,x+w,y+h); int lineY=y+6-scroll; for(String line:formatted.split("\\n",-1)){ c.drawText(textRenderer,line,x+8,lineY,0xE0E0E0,false); lineY+=textRenderer.fontHeight; } c.disableScissor(); super.render(c,mx,my,delta); }
 @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) { int max=Math.max(0,formatted.split("\\n",-1).length*textRenderer.fontHeight-(Math.min(480,height-90)-12)); scroll=(int)Math.max(0,Math.min(max,scroll-(vertical*textRenderer.fontHeight))); return true; }
}
