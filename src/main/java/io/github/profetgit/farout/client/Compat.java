package io.github.profetgit.farout.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui./*$ gfx*/ GuiGraphicsExtractor /**/;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;

/** Vanilla API differences between the 1.21.x and 26.x targets. */
public final class Compat {
    private Compat() {
    }

    //? if >=26.2 {
    public interface Listener<T> extends net.minecraft.client.OptionInstance.ValueUpdateListener<T> {
    }
    //?} else {
    /*public interface Listener<T> extends java.util.function.Consumer<T> {
    }
    *///?}

    public static Screen screen(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.screen();
        //?} else {
        /*return mc.screen;
        *///?}
    }

    public static void setScreen(Minecraft mc, Screen screen) {
        //? if >=26.2 {
        mc.gui.setScreen(screen);
        //?} else {
        /*mc.setScreen(screen);
        *///?}
    }

    public static void notice(Minecraft mc, net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId id, net.minecraft.network.chat.Component title, net.minecraft.network.chat.Component message) {
        //? if >=26.2 {
        net.minecraft.client.gui.components.toasts.SystemToast.add(mc.gui.toastManager(), id, title, message);
        //?} else if >=1.21.2 {
        /*net.minecraft.client.gui.components.toasts.SystemToast.add(mc.getToastManager(), id, title, message);
        *///?} else {
        /*net.minecraft.client.gui.components.toasts.SystemToast.add(mc.getToasts(), id, title, message);
        *///?}
    }

    public static int argb(int alpha, int rgb) {
        //? if >=1.21.2 {
        return net.minecraft.util.ARGB.color(alpha, rgb);
        //?} else if >=1.21 {
        /*return net.minecraft.util.FastColor.ARGB32.color(alpha, rgb);
        *///?} else {
        /*return (alpha << 24) | (rgb & 0xFFFFFF);
        *///?}
    }

    public static Camera camera(Minecraft mc) {
        //? if >=26.2 {
        return mc.gameRenderer.mainCamera();
        //?} else {
        /*return mc.gameRenderer.getMainCamera();
        *///?}
    }

    public static com.mojang.blaze3d.pipeline.RenderTarget renderTarget(Minecraft mc) {
        //? if >=26.2 {
        return mc.gameRenderer.mainRenderTarget();
        //?} else {
        /*return mc.getMainRenderTarget();
        *///?}
    }

    public static BossHealthOverlay boss(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.hud.getBossOverlay();
        //?} else {
        /*return mc.gui.getBossOverlay();
        *///?}
    }

    public static ChatComponent chat(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.hud.getChat();
        //?} else {
        /*return mc.gui.getChat();
        *///?}
    }

    public static void centered(/*$ gfx*/ GuiGraphicsExtractor /**/ g, Font font, String text, int x, int y, int color) {
        //? if >=26.2 {
        g.centeredText(font, text, x, y, color);
        //?} else {
        /*g.drawCenteredString(font, text, x, y, color);
        *///?}
    }

    public static org.joml.Vector3fc look(Camera cam) {
        //? if >=1.21.11 {
        return cam.forwardVector();
        //?} else {
        /*return cam.getLookVector();
        *///?}
    }

    public static long window(Minecraft mc) {
        //? if >=1.21.9 {
        return mc.getWindow().handle();
        //?} else {
        /*return mc.getWindow().getWindow();
        *///?}
    }

    public static Screen chatScreen() {
        //? if >=1.21.9 {
        return new net.minecraft.client.gui.screens.ChatScreen("", false);
        //?} else {
        /*return new net.minecraft.client.gui.screens.ChatScreen("");
        *///?}
    }

    public static String playerName(Minecraft mc) {
        //? if >=1.21.9 {
        return mc.player.getGameProfile().name();
        //?} else {
        /*return mc.player.getGameProfile().getName();
        *///?}
    }

    public static void header(net.minecraft.client.gui.components.OptionsList list, String text) {
        //? if >=1.21.11 {
        list.addHeader(net.minecraft.network.chat.Component.literal(text));
        //?}
    }

    public static void vignette(/*$ gfx*/ GuiGraphicsExtractor /**/ g, net.minecraft.resources./*$ id*/ Identifier /**/ texture, float s) {
        int w = g.guiWidth(), h = g.guiHeight();
        //? if >=1.21.6 {
        g.blit(net.minecraft.client.renderer.RenderPipelines.VIGNETTE, texture, 0, 0, 0, 0, w, h, w, h, net.minecraft.util.ARGB.colorFromFloat(1, s, s, s));
        //?} else if >=1.21.2 {
        /*g.blit(net.minecraft.client.renderer.RenderType::vignette, texture, 0, 0, 0, 0, w, h, w, h, net.minecraft.util.ARGB.colorFromFloat(1, s, s, s));
        *///?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ZERO,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
            com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(s, s, s, 1.0F);
        g.blit(texture, 0, 0, -90, 0.0F, 0.0F, w, h, w, h);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        *///?}
    }

    public static net.minecraft.world.phys.Vec3 position(Camera cam) {
        //? if >=1.21.9 {
        return cam.position();
        //?} else {
        /*return cam.getPosition();
        *///?}
    }

    public static int slot(Minecraft mc) {
        //? if >=1.21.5 {
        return mc.player.getInventory().getSelectedSlot();
        //?} else {
        /*return mc.player.getInventory().selected;
        *///?}
    }

    public static void selectSlot(Minecraft mc, int slot) {
        //? if >=1.21.5 {
        mc.player.getInventory().setSelectedSlot(slot);
        //?} else {
        /*mc.player.getInventory().selected = slot;
        *///?}
    }

    public static void screenshot(com.mojang.blaze3d.pipeline.RenderTarget target, java.util.function.Consumer<com.mojang.blaze3d.platform.NativeImage> then) {
        //? if >=1.21.5 {
        net.minecraft.client.Screenshot.takeScreenshot(target, then);
        //?} else {
        /*then.accept(net.minecraft.client.Screenshot.takeScreenshot(target));
        *///?}
    }

    //? if >=1.21 {
    public static net.minecraft.client.DeltaTracker delta(Minecraft mc) {
        //? if >=1.21.2 {
        return mc.getDeltaTracker();
        //?} else {
        /*return mc.getTimer();
        *///?}
    }
    //?}

    /** Real-time frame time in ticks. */
    public static float realtimeTicks(Minecraft mc) {
        //? if >=1.21 {
        return delta(mc).getRealtimeDeltaTicks();
        //?} else {
        /*return mc.getDeltaFrameTime();
        *///?}
    }

    /** The partial tick of the frame being drawn. */
    public static float partial(Minecraft mc) {
        //? if >=1.21 {
        return delta(mc).getGameTimeDeltaPartialTick(false);
        //?} else {
        /*return mc.getFrameTime();
        *///?}
    }

    //? if <1.21 {
    /*public static void small(net.minecraft.client.gui.components.OptionsList target, net.minecraft.client.OptionInstance<?>... options) {
        target.addSmall(options);
    }
    *///?}
}
