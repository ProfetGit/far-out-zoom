package io.github.profetgit.farout.platform.forge;

import io.github.profetgit.farout.FarOut;
import io.github.profetgit.farout.client.ZoomConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(FarOut.MOD_ID)
public final class ForgeEntry {
    public ForgeEntry() {
        FarOut.init("forge");
        if (FMLEnvironment.dist.isClient()) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ZoomConfigScreen(parent)));
        }
    }
}
