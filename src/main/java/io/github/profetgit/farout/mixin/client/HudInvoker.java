package io.github.profetgit.farout.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Hud.class)
public interface HudInvoker {
    @Invoker("extractSpyglassOverlay")
    void farout$spyglass(GuiGraphicsExtractor graphics, float scale);
}
