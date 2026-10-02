package io.github.profetgit.farout.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only: the real scroll and turn code, reachable whatever the runtime names are. */
@Mixin(MouseHandler.class)
public interface MouseHandlerDemoMixin {
    @Invoker("onScroll")
    void farout$scroll(long window, double x, double y);

    //? if >=1.21 {
    @Invoker("turnPlayer")
    void farout$turn(double delta);
    //?} else {
    /*@Invoker("turnPlayer")
    void farout$turn();
    *///?}

    @Accessor("accumulatedDX")
    void farout$dx(double v);

    @Accessor("accumulatedDY")
    void farout$dy(double v);
}
