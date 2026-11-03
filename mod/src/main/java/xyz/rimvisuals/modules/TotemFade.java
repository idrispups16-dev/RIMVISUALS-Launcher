package xyz.rimvisuals.modules;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import xyz.rimvisuals.core.Easing;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.Setting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;

/** Fades the totem-pop item out during the end of its 40-tick animation. Visual only. */
public final class TotemFade extends Module {
    public final Setting.Num start = add(new Setting.Num("Fade starts at", .6, .1, .95, .05));
    public final Setting.Num endOpacity = add(new Setting.Num("Final opacity", 0, 0, .9, .05));
    public final Setting.Mode easing = add(new Setting.Mode("Easing", 3, Easing.NAMES));

    public TotemFade() {
        super("totem_fade", "Totem Fade", "Fades the totem-pop item out near the end of its animation (works with Totem Scale). Visual only.", Category.VISUALS);
        enabled = false;
    }

    /** Opacity at animation progress 0..1. */
    public float alpha(float progress) {
        if (!isActive()) return 1f;
        float s = (float) start.value;
        if (progress <= s) return 1f;
        return Easing.lerp(1f, (float) endOpacity.value, Easing.apply(easing.index, (progress - s) / (1f - s)));
    }

    /**
     * Wraps the queue so the item's quads are submitted through a translucent layer with this alpha.
     * Detection of the item call is by shape (9 arguments: matrices, context, light, overlay, outline, tints, quads, layer, glint),
     * not by name, so it also works with the intermediary names used outside the dev environment.
     */
    @SuppressWarnings("unchecked")
    public static OrderedRenderCommandQueue fading(OrderedRenderCommandQueue real, float alpha) {
        return (OrderedRenderCommandQueue) Proxy.newProxyInstance(TotemFade.class.getClassLoader(), new Class<?>[]{OrderedRenderCommandQueue.class}, (proxy, m, args) -> {
            if (args != null && args.length == 9 && args[0] instanceof MatrixStack ms && args[6] instanceof List<?> quads && args[7] instanceof RenderLayer) {
                int light = (Integer) args[2], overlay = (Integer) args[3];
                real.submitCustom(ms, TexturedRenderLayers.getItemTranslucentCull(), (entry, vc) -> {
                    for (BakedQuad q : (List<BakedQuad>) quads) vc.quad(entry, q, 1f, 1f, 1f, alpha, light, overlay);
                });
                return null;
            }
            try { return m.invoke(real, args); } catch (InvocationTargetException e) { throw e.getCause(); }
        });
    }
}
