#!/usr/bin/env python3
"""Compose the Crow's Nest icon from the Blockbench renders in dev/icon/frames/ (pack-icon-animation skill).

The mast's ground shadow is rendered in a key colour (#FF00FF, the shadow sprite); it is mapped to the background's shadow
tone here and drawn under the dark outline, which hugs only the solid art.

  python3 dev/make_icon.py [--bg ocean|dusk|sea|coral|cream] [--options]
  --options writes dev/icon/bg_options.png: every background with 4 frames, 96 px thumbnails and its GIF size.

Outputs (dev/icon/out/, since ./gradlew dist may clear dist/):
  icon-animated.gif  Modrinth icon, 240 px, <= 256 KiB, exact palette
  icon-512.png       still (STILL_FRAME)
  src/main/resources/assets/crows_nest/icon.png          the mod's own icon (128 px still)
  dev/icon/contact.png                                    frame sheet for review
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
OUT = ICON / "out"
MOD_ICON = ROOT / "src/main/resources/assets/crows_nest/icon.png"
S = 512
FPS = 25
# the zoom bubble open on the magnified creeper: the frame that says what the mod does
STILL_FRAME = 40
GIF_SIZE = 256
GIF_LIMIT = 256 * 1024
OUTLINE = 17
INK = (10, 26, 44)
# background, ground shadow
BGS = {
    "ocean": ((0x23, 0x50, 0x7E), (0x1A, 0x3D, 0x62)),
    "dusk": ((0x4B, 0x3D, 0x7A), (0x39, 0x2D, 0x5E)),
    "sea": ((0x2F, 0x6B, 0x5E), (0x23, 0x52, 0x48)),
    "coral": ((0xC4, 0x60, 0x4A), (0x9E, 0x47, 0x37)),
    "cream": ((0xE6, 0xD3, 0xA8), (0xC9, 0xB3, 0x86)),
}
BG, SHADOW = BGS["ocean"]
KEY = (255, 0, 255)

CROP = None     # fixed (x0, y0, side) in render pixels, or None to derive from the frames
CROP_MARGIN = 1.1
CROP_PCT = 0.95


def split(im: Image.Image) -> tuple:
    """(solid art RGBA, under-layer RGBA) with the key colours mapped."""
    a = np.array(im.convert("RGBA"))
    under = np.zeros_like(a)
    m = (a[..., 3] > 0) & (a[..., 0] == KEY[0]) & (a[..., 1] == KEY[1]) & (a[..., 2] == KEY[2])
    under[m] = SHADOW + (255,)
    a[m] = 0
    return Image.fromarray(a), Image.fromarray(under)


def crop_box(raws: list) -> tuple:
    if CROP:
        x0, y0, side = CROP
        return (x0, y0, x0 + side, y0 + side)
    boxes = [split(im)[0].getchannel("A").getbbox() for im in raws]
    boxes = [b for b in boxes if b]
    # robust box: the bubble's poof may reach past it for a moment
    lo = lambda v: sorted(v)[int(len(v) * (1 - CROP_PCT))]
    hi = lambda v: sorted(v)[int(len(v) * CROP_PCT) - 1]
    x0, y0 = lo([b[0] for b in boxes]), lo([b[1] for b in boxes])
    x1, y1 = hi([b[2] for b in boxes]), hi([b[3] for b in boxes])
    side = int(max(x1 - x0, y1 - y0) * CROP_MARGIN)
    cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
    return (cx - side // 2, cy - side // 2, cx - side // 2 + side, cy - side // 2 + side)


def compose(im: Image.Image, box: tuple, size: int = S, bg: Image.Image = None) -> Image.Image:
    solid, under = (x.crop(box).resize((size, size), Image.NEAREST) for x in split(im))
    base = bg.copy() if bg is not None else Image.new("RGBA", (size, size), BG + (255,))
    base = Image.alpha_composite(base, under)
    ink = Image.new("RGBA", (size, size), INK + (0,))
    ink.putalpha(solid.getchannel("A").filter(ImageFilter.MaxFilter(OUTLINE if size == S else OUTLINE * size // S | 1)))
    return Image.alpha_composite(Image.alpha_composite(base, ink), solid)


def write_gif(frames: list, out: Path) -> None:
    """Exact palette (every colour kept); slot 255 is transparent and marks every pixel unchanged since the previous frame
    (disposal 1 keeps what is underneath), and Pillow's optimize then crops each frame to the box that still changes."""
    stack = np.stack([np.array(f.convert("RGB")) for f in frames]).astype(np.uint32)
    code = (stack[..., 0] << 16) | (stack[..., 1] << 8) | stack[..., 2]
    colours = np.unique(code)
    if len(colours) > 255:
        sys.exit(f"{len(colours)} colours - more than a GIF palette holds next to the transparent slot")
    idx = np.searchsorted(colours, code).astype(np.uint8)
    pal = []
    for c in colours:
        pal += [int(c >> 16) & 255, int(c >> 8) & 255, int(c) & 255]
    pal += [0, 0, 0] * (255 - len(colours)) + [255, 0, 255]
    ims = []
    for i in range(len(frames)):
        q = idx[i].copy()
        if i:
            q[idx[i] == idx[i - 1]] = 255
        p = Image.fromarray(q, "P")
        p.putpalette(pal)
        ims.append(p)
    ims[0].save(out, save_all=True, append_images=ims[1:], duration=1000 // FPS, loop=0, optimize=True, disposal=1,
                transparency=255)
    # Pillow merges identical consecutive frames into one longer frame: expand by duration when checking
    back, i = Image.open(out), 0
    for n in range(back.n_frames):
        back.seek(n)
        got = np.array(back.convert("RGB"))
        for _ in range(round(back.info["duration"] * FPS / 1000)):
            want = np.array(frames[i].convert("RGB"))
            if not np.array_equal(got, want):
                sys.exit(f"{out.name}: frame {i} decodes differently ({(got != want).any(-1).sum()} px)")
            i += 1
    if i != len(frames):
        sys.exit(f"{out.name}: {i} frames decoded, {len(frames)} expected")
    print(f"{out.name}: {len(colours)} colours, {i} frames verified")


def options(raws: list, box: tuple) -> None:
    """Every background: 4 frames at 256, 96 px thumbnails on dark and light, and the GIF size."""
    global BG, SHADOW
    from PIL import ImageDraw
    picks = [0, 22, STILL_FRAME, 52]
    rows = []
    for name, (bg, sh) in BGS.items():
        BG, SHADOW = bg, sh
        frames = [compose(im, box) for im in raws]
        tmp = OUT / f".opt-{name}.gif"
        write_gif([f.convert("RGB").resize((GIF_SIZE, GIF_SIZE), Image.NEAREST) for f in frames], tmp)
        kib = tmp.stat().st_size / 1024
        tmp.unlink()
        row = Image.new("RGB", (4 * 256 + 2 * 110 + 40, 290), (20, 24, 34))
        d = ImageDraw.Draw(row)
        for i, n in enumerate(picks):
            row.paste(frames[n].convert("RGB").resize((256, 256), Image.NEAREST), (i * 256, 0))
        for j, back in enumerate([(20, 24, 34), (240, 240, 240)]):
            tile = Image.new("RGB", (110, 110), back)
            tile.paste(frames[STILL_FRAME].convert("RGB").resize((96, 96), Image.LANCZOS), (7, 7))
            row.paste(tile, (4 * 256 + 20 + j * 110, 70))
        d.text((6, 262), f"{name}  #{bg[0]:02X}{bg[1]:02X}{bg[2]:02X}   GIF {kib:.0f} KiB", fill=(240, 240, 240))
        rows.append(row)
    sheet = Image.new("RGB", (rows[0].width, len(rows) * rows[0].height))
    for i, r in enumerate(rows):
        sheet.paste(r, (0, i * r.height))
    sheet.save(ICON / "bg_options.png")
    print(ICON / "bg_options.png")


def main() -> None:
    global BG, SHADOW
    args = sys.argv[1:]
    if "--bg" in args:
        BG, SHADOW = BGS[args[args.index("--bg") + 1]]
    files = sorted((ICON / "frames").glob("frame_*.png"))
    if not files:
        sys.exit("no frames in dev/icon/frames - render them from Blockbench first")
    raws = [Image.open(f).convert("RGBA") for f in files]
    box = crop_box(raws)
    print("crop", box, "side", box[2] - box[0])
    OUT.mkdir(parents=True, exist_ok=True)
    if "--options" in args:
        options(raws, box)
        return
    frames = [compose(im, box) for im in raws]

    frames[STILL_FRAME].convert("RGB").save(OUT / "icon-512.png", optimize=True)
    MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
    frames[STILL_FRAME].convert("RGB").resize((128, 128), Image.NEAREST).save(MOD_ICON, optimize=True)

    small = [f.convert("RGB").resize((GIF_SIZE, GIF_SIZE), Image.NEAREST) for f in frames]
    out = OUT / "icon-animated.gif"
    write_gif(small, out)

    cols = 10
    rows = (len(frames) + cols - 1) // cols
    contact = Image.new("RGB", (cols * 128, rows * 128))
    for i, f in enumerate(frames):
        contact.paste(f.convert("RGB").resize((128, 128), Image.LANCZOS), ((i % cols) * 128, (i // cols) * 128))
    contact.save(ICON / "contact.png")

    size = out.stat().st_size
    print(f"{len(frames)} frames @ {FPS} fps -> {out.relative_to(ROOT)} {size / 1024:.0f} KiB "
          f"({'OK' if size <= GIF_LIMIT else 'OVER'} Modrinth 256 KiB limit); still = frame {STILL_FRAME}")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
