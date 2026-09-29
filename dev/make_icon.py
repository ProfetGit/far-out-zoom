#!/usr/bin/env python3
"""Far Out Zoom icon: the view through a brass scope (pack-icon-animation skill, pure 2D like Tidy Pockets' icon).

Every frame is rasterised from world coordinates on a 64 x 64 texel canvas, so the zoom stays pixel-crisp at any
magnification: a canvas texel p shows the world point Tw + (p - Tc) / m, with m on the mod's own log-space spring.
The gag (3.2 s, 25 fps, 80 frames): a tiny creeper on a far hill; the scope breathes and zooms in with speed lines
(the focus ring turns), the creeper fills the glass, notices ("!"), hisses (two white flashes), the view panics back
out, and far away the creeper goes off as a harmless little boom. A new one pops up on the hill.

  python3 dev/make_icon.py [--bg plum|ocean|sunset|navy] [--options]

Sprites (creeper, "!", boom, puff, backgrounds, lettering): dev/icon/draw_sprites.lua. Outputs in dev/icon/out/
(icon-animated.gif 256 px, icon-512.png), the jar icon src/main/resources/assets/far_out_zoom/icon.png, dev/icon/contact.png.
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ICON = ROOT / "dev" / "icon"
SPR = ICON / "sprites"
OUT = ICON / "out"
MOD_ICON = ROOT / "src/main/resources/assets/far_out_zoom/icon.png"
N = 64
FPS, FRAMES = 25, 80
LEN = FRAMES / FPS
STILL_FRAME = 36          # zoomed in on the creeper
GIF_SIZE, GIF_LIMIT = 256, 256 * 1024

BGS = {
    "plum": ((0x4A, 0x20, 0x45), (0x36, 0x16, 0x31)),
    "ocean": ((0x23, 0x50, 0x7E), (0x1A, 0x3D, 0x62)),
    "sunset": ((0xE0, 0x77, 0x3A), (0xBE, 0x5A, 0x26)),
    "navy": ((0x1A, 0x21, 0x40), (0x10, 0x15, 0x2C)),
}
BG, SHADOW = BGS["plum"]
INK = (0x16, 0x06, 0x0F)

C = np.array([31.5, 31.5])      # scope centre
R_OUT, R_IN = 27.5, 23.0        # brass ring, glass
M = 5.0                         # magnification of the hold (the creeper sprite at native size)

T = dict(breath=0.48, zin=0.64, alert=1.56, hiss1=1.84, hiss2=1.96, zout=2.04, boom=2.24, respawn=2.84)

# ---- colours ----
H = lambda s: tuple(int(s[i:i + 2], 16) for i in (1, 3, 5))
SKY = [H("#5FAFE8"), H("#7EC3F2"), H("#A3D6F8"), H("#CBE9FF")]
FAR = [H("#9BD27A"), H("#74B862"), H("#62A656")]
NEAR = [H("#86D06A"), H("#56A24A"), H("#44903F"), H("#377A36")]
CLOUD = [H("#FFFFFF"), H("#E3F1FC")]
BRASS = [H("#4E2F10"), H("#7C5018"), H("#AA7626"), H("#D5A444"), H("#F0CE78"), H("#FFF0BE")]
WHITE = (255, 255, 255)


def load(name):
    return np.array(Image.open(SPR / f"{name}.png").convert("RGBA"))


SPRITES = {n: load(n) for n in ("creeper", "creeper_flash", "fx_alert", "boom", "fx_puff")}
SPRITES["fx_alert_ink"] = SPRITES["fx_alert"].copy()
SPRITES["fx_alert_ink"][..., :3] = (0x16, 0x06, 0x0F)

# ---- the world (1x = canvas texels) ----
far_h = lambda x: 35.0 + 1.6 * math.sin(0.33 * x) + 0.9 * math.sin(0.81 * x + 1.0)
near_h = lambda x: 47.0 + 2.2 * math.sin(0.17 * x + 0.6) + 0.8 * math.sin(0.5 * x)
FEET = np.array([40.0, 0.0])
FEET[1] = far_h(FEET[0])
CH, CW = 26 / M, 8 / M                      # creeper size in world units
TW = FEET - np.array([0, CH / 2])           # zoom target: the creeper's middle
FOCUS = np.array([31.5, 33.5])              # where the zoomed creeper ends up on the canvas
CLOUDS = [(18.0, 17.0, 3.2, 1.3), (22.0, 16.0, 2.2, 1.2)]


# ---- motion ----
def lerp(a, b, u):
    return a + (b - a) * u


def smooth(u):
    u = min(1.0, max(0.0, u))
    return u * u * (3 - 2 * u)


def keys(t, ks):
    """Piecewise smoothstep through [(t, v), ...]."""
    if t <= ks[0][0]:
        return ks[0][1]
    for (t0, v0), (t1, v1) in zip(ks, ks[1:]):
        if t <= t1:
            return lerp(v0, v1, smooth((t - t0) / (t1 - t0)))
    return ks[-1][1]


def spring_track():
    """ln(magnification) per frame: in at T['zin'] (zeta 0.7, the mod's feel), panic out at T['zout'] (fast, near critical)."""
    u = v = 0.0
    out = []
    h = 1 / 960
    for f in range(FRAMES + 1):
        out.append(u)
        t0 = f / FPS
        for i in range(int(1 / FPS / h)):
            t = t0 + i * h
            if t >= T["zout"]:
                target, w, z = 0.0, 26.0, 0.9
            elif t >= T["zin"]:
                target, w, z = math.log(M), 13.0, 0.7
            else:
                target, w, z = 0.0, 13.0, 1.0
            a = w * w * (target - u) - 2 * z * w * v
            v += a * h
            u += v * h
            if t >= T["zout"] and u < 0:
                u, v = 0.0, 0.0
    out[-1] = out[0]
    return out


LNM = spring_track()


def state(f):
    t = f / FPS
    m = math.exp(LNM[f])
    k = min(1.0, max(0.0, (m - 1) / (M - 1)))
    tc = TW + (FOCUS - TW) * k
    breath = keys(t, [(0, 1.0), (T["breath"], 1.0), (T["breath"] + 0.08, 0.95), (T["zin"] + 0.04, 1.025), (T["zin"] + 0.16, 1.0),
                      (T["zout"], 1.0), (T["zout"] + 0.04, 1.03), (T["zout"] + 0.16, 1.0), (LEN, 1.0)])
    hiss = any(t0 <= t < t0 + 0.08 for t0 in (T["hiss1"], T["hiss2"]))
    swell = 1.08 if hiss else 1.0
    creeper = t < T["boom"] or t >= T["respawn"]
    pop = keys(t, [(T["respawn"], 0.0), (T["respawn"] + 0.08, 1.3), (T["respawn"] + 0.12, 0.88), (T["respawn"] + 0.2, 1.0)]) \
        if t >= T["respawn"] else 1.0
    return dict(t=t, m=m, tc=tc, breath=breath, hiss=hiss, swell=swell, creeper=creeper, pop=pop)


# ---- drawing ----
def blit(img, spr, x0, y0, sx, sy, clip=None):
    """Draw a sprite scaled (nearest) with its top-left at canvas (x0, y0); clip(x, y) -> bool keeps a texel."""
    if sx <= 0.01 or sy <= 0.01:
        return
    h, w = spr.shape[:2]
    for y in range(max(0, int(math.floor(y0))), min(N, int(math.ceil(y0 + h * sy)) + 1)):
        for x in range(max(0, int(math.floor(x0))), min(N, int(math.ceil(x0 + w * sx)) + 1)):
            u, v = (x + 0.5 - x0) / sx, (y + 0.5 - y0) / sy
            if 0 <= u < w and 0 <= v < h:
                c = spr[int(v), int(u)]
                if c[3] > 0 and (clip is None or clip(x, y)):
                    img[y, x] = c


def frame(f):
    s = state(f)
    t, m, tc = s["t"], s["m"], s["tc"]
    r_out, r_in = R_OUT * s["breath"], R_IN * s["breath"]
    img = np.zeros((N, N, 4), np.uint8)
    inside = lambda x, y: (x + 0.5 - C[0]) ** 2 + (y + 0.5 - C[1]) ** 2 < r_in * r_in

    # the view: sky, cloud, far hills, near meadow
    for y in range(N):
        for x in range(N):
            if not inside(x, y):
                continue
            wx, wy = TW + (np.array([x + 0.5, y + 0.5]) - tc) / m
            nh, fh = near_h(wx), far_h(wx)
            if wy >= nh:
                d = (wy - nh) * m
                col = NEAR[0] if d < 1 else NEAR[1] if d < 4 else NEAR[2] if d < 9 else NEAR[3]
            elif wy >= fh:
                d = (wy - fh) * m
                col = FAR[0] if d < 1 else FAR[1] if d < 5 else FAR[2]
            else:
                col = SKY[min(3, max(0, int((wy - 8) // 8)))]
                for cx, cy, rx, ry in CLOUDS:
                    if ((wx - cx) / rx) ** 2 + ((wy - cy) / ry) ** 2 < 1:
                        col = CLOUD[1] if wy > cy + 0.4 * ry else CLOUD[0]
            img[y, x] = col + (255,)

    # the creeper (feet on the far hill), scaled with the zoom; hiss = flash + swell; respawn = pop from the ground
    if s["creeper"]:
        sc = m / M
        feet = tc + (FEET - TW) * m
        sx, sy = sc * s["swell"], sc * s["swell"] * s["pop"]
        spr = SPRITES["creeper_flash" if s["hiss"] else "creeper"]
        if sy > 0.01:
            blit(img, spr, feet[0] - 4 * sx, feet[1] - 26 * sy, sx, sy, inside)
        # "!" beside its head while it stares back (inked so it reads on the sky)
        if T["alert"] <= t < T["zout"]:
            a = keys(t, [(T["alert"], 0.4), (T["alert"] + 0.04, 1.4), (T["alert"] + 0.08, 0.9), (T["alert"] + 0.12, 1.0)])
            ax, ay = feet[0] + 4 * sx + 4, feet[1] - 26 * sy + 4.5
            x0, y0 = ax - 2 * a, ay - 4.5 * a
            for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                blit(img, SPRITES["fx_alert_ink"], x0 + dx, y0 + dy, a, a, inside)
            blit(img, SPRITES["fx_alert"], x0, y0, a, a, inside)

    # the far boom and its smoke
    if T["boom"] <= t < T["boom"] + 0.24:
        b = keys(t, [(T["boom"], 0.5), (T["boom"] + 0.04, 1.35), (T["boom"] + 0.08, 1.0), (T["boom"] + 0.16, 0.8), (T["boom"] + 0.24, 0.0)])
        blit(img, SPRITES["boom"], FEET[0] - 3.5 * b, FEET[1] - 4 - 3.5 * b, b, b, inside)
    if T["boom"] + 0.12 <= t < T["boom"] + 0.56:
        u = (t - T["boom"] - 0.12) / 0.44
        p = 0.55 * (1 - u) + 0.15
        blit(img, SPRITES["fx_puff"], FEET[0] - 8 * p, FEET[1] - 5 - 5 * u - 8 * p, p, p, inside)

    # speed lines: outward while zooming in, inward while panicking out
    for t0, outward in ((T["zin"], True), (T["zout"], False)):
        if t0 <= t < t0 + 0.2:
            u = (t - t0) / 0.2
            u = u if outward else 1 - u
            for i in range(14):
                ang = 2 * math.pi * (i + 0.37 * (i % 3)) / 14
                r0, r1 = 5 + 16 * u, 5 + 16 * u + 7 * (1 - abs(0.5 - u))
                for rr in np.arange(r0, r1, 0.5):
                    x, y = int(C[0] + rr * math.cos(ang)), int(C[1] + rr * math.sin(ang))
                    if 0 <= x < N and 0 <= y < N and inside(x, y):
                        img[y, x] = WHITE + (255,)

    # glass glare (static) and the reticle ticks
    for a in range(200, 246, 3):
        r = math.radians(a)
        x, y = int(C[0] + (r_in - 3.2) * math.cos(r)), int(C[1] + (r_in - 3.2) * math.sin(r))
        if inside(x, y):
            img[y, x] = WHITE + (255,)
    for ang in (0, 90, 180, 270):
        r = math.radians(ang)
        for rr in (r_in - 2.0, r_in - 1.0):
            x, y = int(C[0] + rr * math.cos(r)), int(C[1] + rr * math.sin(r))
            if inside(x, y):
                img[y, x] = BRASS[0] + (255,)

    # brass ring: lit from the upper left, a notched focus ring that turns with the zoom
    turn = math.degrees(math.log(m)) * 1.4
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - C[0], y + 0.5 - C[1]
            r = math.hypot(dx, dy)
            if r_in <= r < r_out:
                q = (r - r_in) / (r_out - r_in)
                lit = -(dx + dy) / (r * math.sqrt(2))
                i = 2 + round(1.6 * lit) + (1 if 0.3 < q < 0.7 else 0)
                if q < 0.2:
                    i = 1
                ang = (math.degrees(math.atan2(dy, dx)) - turn) % 22.5
                if q > 0.55 and ang < 4:
                    i -= 2
                img[y, x] = BRASS[max(0, min(5, i))] + (255,)
    return img, r_out


def compose(img, r_out, size, bg=None):
    """Background, drop shadow (+2 texels), ink ring, then the scope, scaled nearest to size."""
    bgc, shc = bg or (BG, SHADOW)
    out = np.empty((N, N, 3), np.uint8)
    out[:] = bgc
    yy, xx = np.mgrid[0:N, 0:N] + 0.5
    rr = np.hypot(xx - C[0] - 1.5, yy - C[1] - 1.5)
    out[rr < r_out + 1] = shc
    rr0 = np.hypot(xx - C[0], yy - C[1])
    out[rr0 < r_out + 1] = INK
    a = img[..., 3] > 0
    out[a] = img[..., :3][a]
    return Image.fromarray(out).resize((size, size), Image.NEAREST)


def write_gif(frames, out):
    """Exact palette; slot 255 transparent marks unchanged pixels (disposal 1), then every frame is decoded and checked."""
    stack = np.stack([np.array(f.convert("RGB")) for f in frames]).astype(np.uint32)
    code = (stack[..., 0] << 16) | (stack[..., 1] << 8) | stack[..., 2]
    colours = np.unique(code)
    if len(colours) > 255:
        sys.exit(f"{len(colours)} colours")
    idx = np.searchsorted(colours, code).astype(np.uint8)
    pal = [v for c in colours for v in (int(c >> 16) & 255, int(c >> 8) & 255, int(c) & 255)]
    pal += [0, 0, 0] * (255 - len(colours)) + [255, 0, 255]
    ims = []
    for i in range(len(frames)):
        q = idx[i].copy()
        if i:
            q[idx[i] == idx[i - 1]] = 255
        p = Image.fromarray(q, "P")
        p.putpalette(pal)
        ims.append(p)
    ims[0].save(out, save_all=True, append_images=ims[1:], duration=1000 // FPS, loop=0, optimize=True, disposal=1, transparency=255)
    back, i = Image.open(out), 0
    for n in range(back.n_frames):
        back.seek(n)
        got = np.array(back.convert("RGB"))
        for _ in range(round(back.info["duration"] * FPS / 1000)):
            if not np.array_equal(got, np.array(frames[i].convert("RGB"))):
                sys.exit(f"{out.name}: frame {i} decodes differently")
            i += 1
    if i != len(frames):
        sys.exit(f"{out.name}: {i} frames decoded, {len(frames)} expected")
    return len(colours)


def render_all():
    return [frame(f) for f in range(FRAMES)]


def options(raw):
    from PIL import ImageDraw
    rows = []
    for name, bg in BGS.items():
        frames = [compose(im, r, 256, bg) for im, r in raw]
        tmp = OUT / f".opt-{name}.gif"
        write_gif(frames, tmp)
        kib = tmp.stat().st_size / 1024
        tmp.unlink()
        row = Image.new("RGB", (4 * 256 + 240, 290), (20, 24, 34))
        for i, n in enumerate([0, 18, STILL_FRAME, 58]):
            row.paste(frames[n], (i * 256, 0))
        for j, back in enumerate([(20, 24, 34), (240, 240, 240)]):
            tile = Image.new("RGB", (110, 110), back)
            tile.paste(frames[STILL_FRAME].resize((96, 96), Image.LANCZOS), (7, 7))
            row.paste(tile, (4 * 256 + 10 + j * 115, 70))
        ImageDraw.Draw(row).text((6, 264), f"{name}  #{bg[0][0]:02X}{bg[0][1]:02X}{bg[0][2]:02X}   GIF {kib:.0f} KiB", fill=(240, 240, 240))
        rows.append(row)
    sheet = Image.new("RGB", (rows[0].width, len(rows) * 290))
    for i, r in enumerate(rows):
        sheet.paste(r, (0, i * 290))
    sheet.save(ICON / "bg_options.png")
    print(ICON / "bg_options.png")


def main():
    global BG, SHADOW
    args = sys.argv[1:]
    if "--bg" in args:
        BG, SHADOW = BGS[args[args.index("--bg") + 1]]
    OUT.mkdir(parents=True, exist_ok=True)
    raw = render_all()
    if not np.array_equal(raw[0][0], frame(FRAMES)[0]):
        sys.exit("loop seam: frame 80 differs from frame 0")
    if "--options" in args:
        options(raw)
        return
    big = [compose(im, r, 512) for im, r in raw]
    big[STILL_FRAME].save(OUT / "icon-512.png", optimize=True)
    MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
    compose(*raw[STILL_FRAME], 128).save(MOD_ICON, optimize=True)
    small = [compose(im, r, GIF_SIZE) for im, r in raw]
    out = OUT / "icon-animated.gif"
    n = write_gif(small, out)
    contact = Image.new("RGB", (10 * 128, 8 * 128))
    for i, fr in enumerate(big):
        contact.paste(fr.resize((128, 128), Image.LANCZOS), ((i % 10) * 128, (i // 10) * 128))
    contact.save(ICON / "contact.png")
    size = out.stat().st_size
    print(f"{FRAMES} frames, {n} colours -> {out.relative_to(ROOT)} {size / 1024:.0f} KiB "
          f"({'OK' if size <= GIF_LIMIT else 'OVER'} 256 KiB); seam exact; still = frame {STILL_FRAME}")
    if size > GIF_LIMIT:
        sys.exit(1)


if __name__ == "__main__":
    main()
