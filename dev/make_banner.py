#!/usr/bin/env python3
"""Compose the Far Out Zoom description banner (1536x512, 3:1): banner_bg x8, the title and tagline, and the scope animation
from make_icon.frame() scaled x7 on the right. Writes dev/icon/out/banner.png (STILL_FRAME) and banner-animated.gif
(exact palette, slot 255 = stepped corners + unchanged pixels, every frame decode-checked). Copy the GIF to docs/banner.gif."""
import shutil
import sys
from pathlib import Path

import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import make_icon as mi  # noqa: E402

ROOT = mi.ROOT
W, H = 1536, 512
ART_SCALE = 7
ART_CENTRE = (1232, 256)
# FAR OUT (sunset) + ZOOM (sky) x8 on one line, a 4-cell word gap; the tagline x6 under it
LAYERS = (("banner_title", 8, (72, 152)), ("banner_title_zoom", 8, (72 + 67 * 8, 152)), ("banner_tagline", 6, (72, 304)))
GIF_LIMIT = 5 * 1024 * 1024
GRID = 8
CORNER = (4, 2, 1, 1)


def sprite(name, scale):
    im = Image.open(mi.SPR / f"{name}.png").convert("RGBA")
    return im.resize((im.width * scale, im.height * scale), Image.NEAREST)


def scope(img, r_out):
    """The scope with its drop shadow and ink ring on transparency (compose() without the background)."""
    out = np.zeros((mi.N, mi.N, 4), np.uint8)
    yy, xx = np.mgrid[0:mi.N, 0:mi.N] + 0.5
    out[np.hypot(xx - mi.C[0] - 1.5, yy - mi.C[1] - 1.5) < r_out + 1] = mi.SHADOW + (255,)
    out[np.hypot(xx - mi.C[0], yy - mi.C[1]) < r_out + 1] = mi.INK + (255,)
    a = img[..., 3] > 0
    out[a] = img[a]
    s = mi.N * ART_SCALE
    return Image.fromarray(out).resize((s, s), Image.NEAREST)


def corner_mask():
    gw, gh = W // GRID, H // GRID
    m = np.ones((gh, gw), bool)
    for row, cut in enumerate(CORNER):
        m[row, :cut] = m[row, gw - cut:] = False
        m[gh - 1 - row, :cut] = m[gh - 1 - row, gw - cut:] = False
    return np.repeat(np.repeat(m, GRID, 0), GRID, 1)


def main():
    bg = sprite("banner_bg", 8)
    for n, s, (x, y) in LAYERS:
        bg.alpha_composite(sprite(n, s), (x, y))
    pos = (ART_CENTRE[0] - mi.N * ART_SCALE // 2, ART_CENTRE[1] - mi.N * ART_SCALE // 2)
    frames = []
    for f in range(mi.FRAMES):
        out = bg.copy()
        out.alpha_composite(scope(*mi.frame(f)), pos)
        frames.append(out.convert("RGB"))

    mask = corner_mask()
    mi.OUT.mkdir(parents=True, exist_ok=True)
    still = np.array(frames[mi.STILL_FRAME].convert("RGBA"))
    still[~mask, 3] = 0
    Image.fromarray(still).save(mi.OUT / "banner.png", optimize=True)

    stack = np.stack([np.array(f) for f in frames]).astype(np.uint32)
    code = (stack[..., 0] << 16) | (stack[..., 1] << 8) | stack[..., 2]
    colours = np.unique(code)
    if len(colours) > 255:
        sys.exit(f"{len(colours)} colours")
    idx = np.searchsorted(colours, code).astype(np.uint8)
    pal = [v for c in colours for v in (int(c >> 16) & 255, int(c >> 8) & 255, int(c) & 255)]
    pal += [0, 0, 0] * (255 - len(colours)) + [255, 0, 255]
    gif = []
    for i in range(len(frames)):
        q = idx[i].copy()
        if i:
            q[idx[i] == idx[i - 1]] = 255
        q[~mask] = 255
        p = Image.fromarray(q, "P")
        p.putpalette(pal)
        gif.append(p)
    out = mi.OUT / "banner-animated.gif"
    gif[0].save(out, save_all=True, append_images=gif[1:], duration=1000 // mi.FPS, loop=0, optimize=True, disposal=1,
                transparency=255)

    back, i = Image.open(out), 0
    for n in range(back.n_frames):
        back.seek(n)
        got = np.array(back.convert("RGBA"))
        for _ in range(round(back.info["duration"] * mi.FPS / 1000)):
            want = np.array(frames[i])
            if not (np.array_equal(got[mask][:, :3], want[mask]) and (got[..., 3][mask] == 255).all()
                    and (got[..., 3][~mask] == 0).all()):
                sys.exit(f"banner GIF frame {i} decodes differently")
            i += 1
    if i != len(frames):
        sys.exit(f"banner GIF: {i} frames decoded, {len(frames)} expected")
    size = out.stat().st_size
    (ROOT / "docs").mkdir(exist_ok=True)
    shutil.copyfile(out, ROOT / "docs" / "banner.gif")
    print(f"banner.png (frame {mi.STILL_FRAME}); {len(frames)} frames, {len(colours)} colours -> {out.relative_to(ROOT)} "
          f"{size / 1024:.0f} KiB ({'OK' if size <= GIF_LIMIT else 'OVER'} 5 MiB), verified; copied to docs/banner.gif")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
