package xyz.rimvisuals.gui;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import xyz.rimvisuals.modules.ShulkerPreview;
public final class ShulkerTooltip implements TooltipComponent {
 private final ShulkerPreview.Data data;
 public ShulkerTooltip(ShulkerPreview.Data data){this.data=data;}
 @Override public int getWidth(TextRenderer font){return 170;}
 @Override public int getHeight(TextRenderer font){return 62;}
 @Override public void drawItems(TextRenderer font,int x,int y,int width,int height,DrawContext c){c.fill(x,y,x+170,y+62,0xff171021);for(int i=0;i<27;i++){int px=x+4+(i%9)*18,py=y+4+(i/9)*18;c.fill(px,py,px+17,py+17,0xff292139);if(i<data.stacks().size()){var s=data.stacks().get(i);if(!s.isEmpty()){c.drawItem(s,px,py);c.drawStackOverlay(font,s,px,py);}}}}
}
