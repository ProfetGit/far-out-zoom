package io.github.profetgit.farout.platform.neoforge;

//? neoforge {
/*import io.github.profetgit.farout.FarOut;
import io.github.profetgit.farout.client.ZoomConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/^* Client only: the settings screen in NeoForge's mod list. *^/
@Mod(value = FarOut.MOD_ID, dist = Dist.CLIENT)
public final class NeoForgeEntry {
    public NeoForgeEntry(ModContainer container) {
        FarOut.init("neoforge");
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ZoomConfigScreen(parent));
    }
}
*///?}
