package io.github.profetgit.farout.platform.fabric;

//? fabric {
import io.github.profetgit.farout.FarOut;
import net.fabricmc.api.ClientModInitializer;

public final class FabricEntry implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FarOut.init("fabric");
    }
}
//?}
