# Changelog

## 0.2.3

- The zoom animation now runs on Minecraft's frame timer instead of the system clock. It feels the same in play, and screen recorders that capture the game frame by frame now show the zoom at its real speed.

## 0.2.2

- Fixed: with Iris shaders on, the hand no longer stayed in view while zoomed (Iris draws the hand its own way).

## 0.2.1

- Renamed to Far Out Zoom (mod id `far_out_zoom`, config `config/far_out_zoom.json`), so zoom searches find it. Never released as Far Out.

## 0.2.0

- Renamed to Far Out (mod id `far_out`, config `config/far_out.json`). The mod was never released under the old name.
- New icon: the view through the scope, zooming in on a far creeper.

## 0.1.1

- Mod icon: the copper golem in a barrel crow's nest, spyglass out, a zoom bubble on a far creeper (Mod Menu, NeoForge and Forge mod lists).

## 0.1.0

First build.

- Zoom with C: hold (default), toggle, or hybrid (hold for a quick look, tap to stay zoomed).
- Springy zoom on real time: the same feel at any frame rate and any magnification, with a small overshoot that settles. Zooming out is quicker.
- Scroll wheel zooms in and out while zoomed, in steps of sqrt 2 (2x, 2.8x, 4x, 5.7x, 8x ... 64x), with a soft click per step. The hotbar slot doesn't change. Double-tap the key to go back to the starting zoom.
- Steady aim: mouse turns are divided by the magnification, so the view moves the same amount on screen at every zoom. Optional cinematic camera while zoomed.
- Sprinting, Speed and drawing a bow don't change the zoomed view.
- The hand slides out of view and view bobbing fades out while zoomed.
- Far mobs: mobs, players and block entities (signs, chests, banners) stay drawn as far out as the zoom brings them. Vanilla stops drawing a cow at about 68 blocks and a sign at 64.
- Clear haze: distance haze and rain fog thin while zoomed. The fog at the edge of the render distance stays.
- Rangefinder: distance and name of what the crosshair points at, out to the edge of the loaded world ("> 224 m" past it). Entities are named by type.
- With Distant Horizons the zoom reaches its LOD terrain, and the rangefinder measures it too (shown as ≈, from DH's own data).
- Zoom level under the crosshair. Optional vignette or spyglass-scope overlay.
- Vanilla's Save Hotbar Activator is also on C. While they share the key it is paused, because zooming and then pressing a number key in creative would overwrite a saved hotbar. A one-time toast says so.
- Settings screen from Mod Menu (Fabric), the mod list (NeoForge, Forge), or an unbound "Zoom settings" key.
- Client-only. Fabric (and Quilt), NeoForge and Forge, Minecraft 26.2 and 26.3.
