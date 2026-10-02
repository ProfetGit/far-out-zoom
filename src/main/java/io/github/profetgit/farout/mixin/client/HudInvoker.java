package io.github.profetgit.farout.mixin.client;

import net.minecraft.client.gui./*$ gfx*/ GuiGraphicsExtractor /**/;
import net.minecraft.client.gui./*$ gui*/ Hud /**/;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(/*$ gui*/ Hud /**/.class)
public interface HudInvoker {
    @Invoker(/*$ spyglass*/ "extractSpyglassOverlay" /**/)
    void farout$spyglass(/*$ gfx*/ GuiGraphicsExtractor /**/ graphics, float scale);
}
