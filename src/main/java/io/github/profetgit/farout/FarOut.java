package io.github.profetgit.farout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entry shared by the loaders. Everything happens on the client; the client setup starts from a Minecraft mixin. */
public final class FarOut {
    public static final String MOD_ID = "far_out_zoom";
    public static final Logger LOG = LoggerFactory.getLogger("Far Out Zoom");
    private static String loader = "?";

    private FarOut() {
    }

    public static void init(String loaderName) {
        loader = loaderName;
        LOG.info("Far Out Zoom loaded on {}", loader);
    }

    public static String loader() {
        return loader;
    }
}
