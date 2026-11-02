package xyz.rimvisuals.modules;

import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import xyz.rimvisuals.core.Easing;
import xyz.rimvisuals.core.Module;
import xyz.rimvisuals.core.PerformanceMonitor;
import xyz.rimvisuals.core.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaves a client-only, non-solid visual copy of a Respawn Anchor model that fades and shrinks after the anchor explodes.
 * The real block is gone; nothing here touches the world, hitboxes or the server.
 * An effect is created only when an explosion packet proves it: a radius-5 explosion centred exactly on a Respawn Anchor
 * block's centre, with the anchor seen in the client world (either just before or just after the block update arrives).
 * Mining or any other removal of an anchor never creates a ghost.
 */
public final class AnchorFade extends Module {
    public final Setting.Num duration = add(new Setting.Num("Duration (ms)", 350, 200, 500, 10));
    public final Setting.Bool fade = add(new Setting.Bool("Fade", true));
    public final Setting.Num startOpacity = add(new Setting.Num("Start opacity", .9, .1, 1, .05));
    public final Setting.Bool shrink = add(new Setting.Bool("Scale", true));
    public final Setting.Num endScale = add(new Setting.Num("End scale", .2, 0, 1, .05));
    public final Setting.Bool tint = add(new Setting.Bool("Tint", false));
    public final Setting.Color tintColor = add(new Setting.Color("Tint color", 0xB36BFF));
    public final Setting.Num tintStrength = add(new Setting.Num("Tint strength", .6, 0, 1, .05));
    public final Setting.Mode easing = add(new Setting.Mode("Easing", 1, Easing.NAMES));

    private record Ghost(BlockPos pos, List<BakedQuad> quads, int light, long startNanos) {}

    private final List<Ghost> ghosts = new ArrayList<>();
    private BlockPos pendingPos;           // explosion centred on an anchor, waiting for its block update
    private long pendingNanos;
    private BlockPos lastGhostPos;
    private long lastGhostNanos;
    private BlockState pendingState;

    public AnchorFade() {
        super("anchor_fade", "Anchor Fade / Scale", "When a Respawn Anchor explodes, a client-only copy of its model fades and shrinks where it stood. Visual only: no block, hitbox or server change.", Category.VISUALS);
        enabled = false;
    }

    /** The block position whose centre this explosion is centred on, or null (needs radius 5 and a centre at block centre ±0.02). */
    public static BlockPos anchorCandidate(Vec3d c, float radius) {
        if (Math.abs(radius - 5.0f) > 1e-3f) return null;
        BlockPos p = BlockPos.ofFloored(c);
        if (Math.abs(c.x - (p.getX() + .5)) > .02 || Math.abs(c.y - (p.getY() + .5)) > .02 || Math.abs(c.z - (p.getZ() + .5)) > .02) return null;
        return p;
    }

    public void onExplosion(ClientWorld world, Vec3d center, float radius) {
        if (!enabled) return;
        BlockPos p = anchorCandidate(center, radius);
        if (p == null) return;
        BlockState s = world.getBlockState(p);
        if (s.isOf(Blocks.RESPAWN_ANCHOR)) spawn(world, p, s);
        else { pendingPos = p; pendingNanos = System.nanoTime(); }
    }

    /** Called before the client applies a block update. */
    public void onBlockUpdate(ClientWorld world, BlockPos pos, BlockState incoming) {
        if (!enabled) return;
        if (pendingPos != null && System.nanoTime() - pendingNanos > 1_500_000_000L) pendingPos = null;
        if (pendingPos == null || !pendingPos.equals(pos)) return;
        BlockState old = world.getBlockState(pos);
        if (old.isOf(Blocks.RESPAWN_ANCHOR) && !incoming.isOf(Blocks.RESPAWN_ANCHOR)) { pendingPos = null; spawn(world, pos, old); }
    }

    private void spawn(ClientWorld world, BlockPos pos, BlockState state) {
        if(xyz.rimvisuals.core.Modules.PERFORMANCE.safe.value)return;
        long now = System.nanoTime();
        if (pos.equals(lastGhostPos) && now - lastGhostNanos < 1_500_000_000L) return; // already handled for this explosion
        if (ghosts.size() >= 8) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        List<BlockModelPart> parts = new ArrayList<>();
        mc.getBlockRenderManager().getModel(state).addParts(Random.create(42L), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockModelPart part : parts) {
            quads.addAll(part.getQuads(null));
            for (Direction d : Direction.values()) quads.addAll(part.getQuads(d));
        }
        if (quads.isEmpty()) return;
        int light = LightmapTextureManager.pack(world.getLightLevel(LightType.BLOCK, pos), world.getLightLevel(LightType.SKY, pos));
        ghosts.add(new Ghost(pos.toImmutable(), quads, light, now));
        lastGhostPos = pos.toImmutable();
        lastGhostNanos = now;
        pendingPos = null;
    }

    /** Opacity (0..1) and scale at progress t (0..1). */
    public float alphaAt(float t) { return fade.value ? Easing.lerp((float) startOpacity.value, 0f, Easing.apply(easing.index, t)) : (t < 1f ? (float) startOpacity.value : 0f); }
    public float scaleAt(float t) { return shrink.value ? Easing.lerp(1f, (float) endScale.value, Easing.apply(easing.index, t)) : 1f; }

    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, Vec3d cam) {
        if(!enabled||xyz.rimvisuals.core.Modules.PERFORMANCE.safe.value){ghosts.clear();pendingPos=null;return;}
        if (ghosts.isEmpty()) return;
        long t0 = PerformanceMonitor.begin();
        long now = System.nanoTime(), dur = (long) (duration.value * 1_000_000L);
        ghosts.removeIf(g -> now - g.startNanos > dur);
        float r = 1f, gr = 1f, b = 1f;
        if (tint.value) {
            float k = (float) tintStrength.value;
            r = 1f + (((tintColor.rgb >> 16) & 255) / 255f - 1f) * k;
            gr = 1f + (((tintColor.rgb >> 8) & 255) / 255f - 1f) * k;
            b = 1f + ((tintColor.rgb & 255) / 255f - 1f) * k;
        }
        for (Ghost g : ghosts) {
            if(g.pos.getSquaredDistance(cam)>48*48)continue;
            float t = (now - g.startNanos) / (float) dur;
            float a = alphaAt(t), s = scaleAt(t);
            if (a <= .01f || s <= .01f) continue;
            final float fr = r, fg = gr, fb = b;
            matrices.push();
            matrices.translate(g.pos.getX() + .5 - cam.x, g.pos.getY() + .5 - cam.y, g.pos.getZ() + .5 - cam.z);
            matrices.scale(s, s, s);
            matrices.translate(-.5, -.5, -.5);
            queue.submitCustom(matrices, RenderLayers.translucentMovingBlock(), (entry, vc) -> {
                for (BakedQuad q : g.quads) vc.quad(entry, q, fr, fg, fb, a, g.light, OverlayTexture.DEFAULT_UV);
            });
            matrices.pop();
        }
        PerformanceMonitor.end("Anchor Fade", t0);
    }
}
