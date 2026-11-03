package xyz.rimvisuals.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import xyz.rimvisuals.auth.DeviceAuth;
import xyz.rimvisuals.core.Gfx;
import java.net.URI;

/** Browser confirmation only: never asks for Google/Microsoft passwords in game. */
public final class AccountScreen extends Screen {
    private final Screen parent;
    private int x, y, w;
    public AccountScreen(Screen parent) { super(Text.literal("RIMVISUALS Account")); this.parent = parent; }
    @Override protected void init() {
        w = Math.min(460, width - 24); x = (width - w)/2; y = Math.max(12, (height-258)/2);
        int bw = Math.min(300,w-24), bx=(width-bw)/2;
        addDrawableChild(ButtonWidget.builder(Text.literal("1. Получить новый код"), b -> DeviceAuth.INSTANCE.start()).dimensions(bx,y+126,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("2. Войти через сайт"), b -> Util.getOperatingSystem().open(URI.create(DeviceAuth.INSTANCE.browserUrl()))).dimensions(bx,y+152,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Продолжить / Вернуться в Minecraft"), b -> close()).dimensions(bx,y+178,bw,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Выйти из аккаунта мода"), b -> DeviceAuth.INSTANCE.logout()).dimensions(bx,y+204,bw,20).build());
    }
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0xf00c0b14);
        Gfx.roundBorder(c,x,y,w,244,16,0xff17151f,0xff493562);
        c.drawCenteredTextWithShadow(textRenderer,"✦ RIMVISUALS",width/2,y+19,0xffbb99ff);
        c.drawCenteredTextWithShadow(textRenderer,DeviceAuth.INSTANCE.active()?"Аккаунт подключён":"Подтвердите вход через сайт",width/2,y+42,0xffe9e0ff);
        String code=DeviceAuth.INSTANCE.code();
        if(!code.isEmpty())code=code.substring(0,4)+"-"+code.substring(4);
        c.drawCenteredTextWithShadow(textRenderer,code.isEmpty()?"Код появится после нажатия кнопки":code,width/2,y+65,0xffc5a2ff);
        String message=DeviceAuth.INSTANCE.message();
        int lineY=y+86;
        for(var line:textRenderer.wrapLines(Text.literal(message),w-32)) { c.drawText(textRenderer,line,x+16,lineY,0xffb9b1c8,false); lineY+=10; }
        super.render(c,mx,my,delta);
        if(Boolean.getBoolean("rimvisuals.smoke"))xyz.rimvisuals.SmokeTest.frames++;
    }
    @Override public void close(){ if(client!=null)client.setScreen(parent); }
    @Override public boolean shouldPause(){ return false; }
}
