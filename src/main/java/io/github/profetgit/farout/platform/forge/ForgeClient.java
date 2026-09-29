package io.github.profetgit.farout.platform.forge;

//? forge {
/*import io.github.profetgit.farout.client.ZoomConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/^* Client only (ForgeEntry calls it on the client): the settings screen in Forge's mod list. *^/
final class ForgeClient {
    private ForgeClient() {
    }

    static void register(FMLJavaModLoadingContext context) {
        context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ZoomConfigScreen(parent)));
    }
}
*///?}
