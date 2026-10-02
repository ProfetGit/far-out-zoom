plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.18.2" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18.2" apply false
    id("net.neoforged.moddev") version "2.0.147" apply false
    id("net.minecraftforge.gradle") version "7.0.40" apply false
}

stonecutter active "26.3-fabric"

stonecutter parameters {
    constants.match(current.project.substringAfterLast('-'), "fabric", "neoforge", "forge")
    val modern = eval(current.version, ">=26.2")
    swaps["gfx"] = if (modern) "GuiGraphicsExtractor" else "GuiGraphics"
    swaps["gui"] = if (modern) "Hud" else "Gui"
    swaps["crosshair"] = if (modern) "\"extractCrosshair\"" else "\"renderCrosshair\""
    swaps["cameraOverlays"] = if (modern) "\"extractCameraOverlays\"" else "\"renderCameraOverlays\""
    swaps["hands"] = if (modern) "\"submitHandsWithItems\"" else "\"renderHandsWithItems\""
    swaps["id"] = if (eval(current.version, ">=1.21.11")) "Identifier" else "ResourceLocation"
    swaps["cow"] = if (eval(current.version, ">=1.21.11")) "cow.Cow" else "Cow"
    swaps["berender"] = if (eval(current.version, ">=1.21.9")) "\"tryExtractRenderState*\"" else "\"render*\""
    swaps["bergen"] = if (eval(current.version, ">=1.21.9")) ", ?" else ""
    swaps["spyglass"] = if (modern) "\"extractSpyglassOverlay\"" else "\"renderSpyglassOverlay\""
}

tasks.register<Copy>("dist") {
    group = "build"
    description = "Builds every target and copies the jars into dist/"
    val skip = providers.gradleProperty("cn.skip").orNull?.split(',')?.map { it.trim() }.orEmpty()
    stonecutter.versions.filter { v -> skip.none { v.project.endsWith("-$it") || v.project == it } }.forEach { v ->
        val p = project(":${v.project}")
        dependsOn(p.tasks.named("build"))
        from(p.layout.buildDirectory.dir("libs")) {
            include("far_out_zoom-*.jar")
            exclude("*-sources.jar", "*-dev.jar")
        }
    }
    into(layout.projectDirectory.dir("dist"))
    doFirst { delete(layout.projectDirectory.dir("dist")) }
}
