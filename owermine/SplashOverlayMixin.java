package deu.crystall.mixins.client.display.loading;

import com.mojang.blaze3d.systems.RenderSystem;
import deu.crystall.utils.display.font.Fonts;
import deu.crystall.utils.display.geometry.Render2D;
import deu.crystall.utils.display.interfaces.QuickImports;
import deu.crystall.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin implements QuickImports {
    private static final int LOADING_FRAMES = 180;
    private static final int LOADING_FPS = 30;

    private static final Identifier BACKGROUND = Identifier.of("minecraft", "loadingscr/background.png");

    @Unique private boolean initialized = false;
    @Unique private long lastRenderTime = 0L;
    @Unique private long overlayCloseRequestedAtMs = -1L;

    @Unique private float pulseTime = 0f;
    @Unique private float indeterminateT = 0f;
    @Unique private float progressGlow = 0f;

    @Unique private static final int PARTICLE_COUNT = 40;
    @Unique private final float[] pX = new float[PARTICLE_COUNT];
    @Unique private final float[] pY = new float[PARTICLE_COUNT];
    @Unique private final float[] pSpeed = new float[PARTICLE_COUNT];
    @Unique private final float[] pSize = new float[PARTICLE_COUNT];
    @Unique private final float[] pAlpha = new float[PARTICLE_COUNT];
    @Unique private final float[] pDrift = new float[PARTICLE_COUNT];

    @Inject(method = "render", at = @At("HEAD"))
    private void renderBlackBackgroundAtHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        // Cover everything (including Mojang logo) with black before vanilla renders
        rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, w, h)
                .color(new Color(0, 0, 0, 255).getRGB())
                .build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCrystalLoading(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return;

        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();

        long now = Util.getMeasuringTimeMs();
        if (!initialized) {
            initialized = true;
            lastRenderTime = now;
            initParticles(w, h);
        }

        float dt = (now - lastRenderTime) / 1000f;
        lastRenderTime = now;
        dt = MathHelper.clamp(dt, 0.001f, 0.1f);

        pulseTime += dt;
        progressGlow += dt * 3f;
        indeterminateT = (indeterminateT + dt * 0.9f) % 1f;

        // Cover everything vanilla rendered between HEAD and TAIL (logo, progress bar)
        RenderSystem.disableDepthTest();
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 2000); // 2000 units above

        rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, w, h)
                .color(new Color(0, 0, 0, 255).getRGB())
                .build());

        drawBackground(context, w, h);
        renderParticles(context, w, h, dt);

        float cx = w / 2f;
        float cy = h / 2f;

        int frame = (int) ((Util.getMeasuringTimeMs() / (1000L / LOADING_FPS)) % LOADING_FRAMES);
        String frameStr = String.format("%03d", frame);

        float size = 120f;
        image.setTexture("loadingscr/" + frameStr + ".png").render(ShapeProperties.create(context.getMatrices(), cx - size / 2f, cy - size / 2f, size, size)
                .color(-1)
                .build());

        float titleY = cy + size / 2f + 10f;
        Fonts.getSize(18, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Crystal-Beta", cx, titleY, new Color(255, 255, 255, 220).getRGB());

        float barY = titleY + 16f;
        drawIndeterminateBar(context, cx, barY);

        context.getMatrices().pop();
        RenderSystem.enableDepthTest();
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MinecraftClient;setOverlay(Lnet/minecraft/client/gui/screen/Overlay;)V"
            )
    )
    private void delayCloseOverlay(MinecraftClient client, Overlay overlay) {
        if (overlay == null) {
            long now = Util.getMeasuringTimeMs();
            if (overlayCloseRequestedAtMs < 0L) {
                overlayCloseRequestedAtMs = now;
                return;
            }
            if (now - overlayCloseRequestedAtMs < 1000L) {
                return;
            }
        }
        client.setOverlay(overlay);
    }

    @Unique
    private void drawBackground(DrawContext context, int w, int h) {
        boolean hasBg = false;
        try {
            hasBg = mc.getResourceManager().getResource(BACKGROUND).isPresent();
        } catch (Exception ignored) {}

        if (hasBg) {
            float zoom = 1.08f;
            float px = (float) Math.sin(pulseTime * 0.55f) * 6f;
            float py = (float) Math.cos(pulseTime * 0.45f) * 5f;
            float zw = w * zoom;
            float zh = h * zoom;
            float x0 = -(zw - w) / 2f + px;
            float y0 = -(zh - h) / 2f + py;
            Render2D.drawTexture(context.getMatrices(), BACKGROUND, x0, x0 + zw, y0, y0 + zh, 0, (int) zw, (int) zh, 0, 0, (int) zw, (int) zh, -1);
        } else {
            rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, w, h)
                    .color(new Color(10, 10, 12, 255).getRGB())
                    .build());
        }

        rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, w, h)
                .color(new Color(0, 0, 0, 100).getRGB())
                .build());
    }

    @Unique
    private void drawIndeterminateBar(DrawContext context, float centerX, float y) {
        float barW = 180f;
        float barH = 3f;
        float barX = centerX - barW / 2f;

        int bgAlpha = 60;
        rectangle.render(ShapeProperties.create(context.getMatrices(), barX, y, barW, barH)
                .round(barH / 2f)
                .color(new Color(255, 255, 255, bgAlpha).getRGB())
                .build());

        float segW = 50f;
        float segX = barX + (barW + segW) * indeterminateT - segW;
        float x0 = Math.max(barX, segX);
        float x1 = Math.min(barX + barW, segX + segW);
        float w0 = x1 - x0;

        if (w0 > 0.5f) {
            int a1 = (int)(200 * 0.85f);
            int a2 = (int)(140 * 0.85f);
            rectangle.render(ShapeProperties.create(context.getMatrices(), x0, y, w0, barH)
                    .round(barH / 2f)
                    .color(
                            new Color(255, 114, 118, a1).getRGB(),
                            new Color(255, 154, 118, a2).getRGB(),
                            new Color(255, 114, 118, a1).getRGB(),
                            new Color(255, 154, 118, a2).getRGB()
                    )
                    .build());
        }

        float glowPos = (float) ((progressGlow * 0.3f) % 1.0);
        float glowX = barX + barW * glowPos;
        float glowW = 20f;
        if (glowX + glowW > barX + barW) glowW = barX + barW - glowX;
        if (glowW > 0.5f) {
            int shineA = 80;
            rectangle.render(ShapeProperties.create(context.getMatrices(), glowX, y, glowW, barH)
                    .round(barH / 2f)
                    .color(new Color(255, 255, 255, shineA).getRGB())
                    .build());
        }
    }

    @Unique
    private void initParticles(int w, int h) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            resetParticle(i, true, w, h);
        }
    }

    @Unique
    private void renderParticles(DrawContext context, int w, int h, float dt) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            updateParticle(i, dt, w, h);
            int a = (int) (pAlpha[i] * 255f);
            if (a <= 0) continue;
            float s = pSize[i];
            rectangle.render(ShapeProperties.create(context.getMatrices(), pX[i], pY[i], s, s)
                    .round(s / 2f)
                    .color(new Color(255, 255, 255, a).getRGB())
                    .build());
        }
    }

    @Unique
    private void resetParticle(int i, boolean randomY, int w, int h) {
        pX[i] = (float) (Math.random() * (w + 200));
        pY[i] = randomY ? (float) (Math.random() * (h + 200)) : (h + 60f + (float) (Math.random() * 120f));
        pSpeed[i] = 8f + (float) (Math.random() * 25f);
        pSize[i] = 1f + (float) (Math.random() * 2.5f);
        pAlpha[i] = 0.08f + (float) (Math.random() * 0.2f);
        pDrift[i] = -15f + (float) (Math.random() * 30f);
    }

    @Unique
    private void updateParticle(int i, float dt, int w, int h) {
        pY[i] -= pSpeed[i] * dt;
        pX[i] += pDrift[i] * dt;
        if (pY[i] < -20f) resetParticle(i, false, w, h);
    }
}
