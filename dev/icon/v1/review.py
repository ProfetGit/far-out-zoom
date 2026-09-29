#!/usr/bin/env python3
"""Texture review sheet for the Crow's Nest icon: every new sprite at 10x, a flat front-view mockup of the key moment
(golem in the barrel crow's nest, spyglass out, the zoom bubble with the creeper) at 8x and at the 96 px the icon is
often seen at, and the banner lettering on the banner background. A 2D approximation, not a Blockbench render.

  python3 dev/icon/review.py [out.png]
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

HERE = Path(__file__).resolve().parent
SPR = HERE / "sprites"
COPIED = ("head_", "nose_", "rod_", "knob_", "body_", "arm_", "eye", "fx_puff", "fx_spark", "shadow")
BG = (35, 80, 126, 255)


def load(name):
    return Image.open(SPR / f"{name}.png").convert("RGBA")


def crop(img, w, h):
    return img.crop((0, 0, w, h))


def mockup(s=8):
    """Front view, 1 unit = s px; y up. Returns the image."""
    W, H = 80, 76
    img = Image.new("RGBA", (W * s, H * s), BG)

    def put(tex, x0, y_top, w, h, scale=1.0):
        t = tex.resize((round(w * s * scale), round(h * s * scale)), Image.NEAREST)
        img.alpha_composite(t, (round(x0 * s), round((H - y_top) * s)))

    X = 14  # mast centre
    shadow = load("shadow").resize((22 * s, 6 * s), Image.NEAREST)
    sh = Image.new("RGBA", shadow.size, (26, 61, 98, 255))
    img.paste(sh, (round((X - 11) * s), round((H - 3) * s)), shadow)
    for y0 in (16, 0):
        put(crop(load("mast_side_s"), 4, 16), X - 2, y0 + 16, 4, 16)
    # flag pole at the back-left corner, the pennant streaming left (away from the bubble)
    put(crop(load("pole"), 1, 16), X - 7, 54, 1, 16)
    put(ImageOps.mirror(load("flag_0")), X - 17, 54, 10, 7)
    put(crop(load("barrel_inner"), 16, 12), X - 8, 44, 16, 12)
    # golem: head resting on the rim (y 38), rod and knob, eyes on head rows 1-2, nose partly below the rim
    put(crop(load("head_front_s"), 8, 5), X - 4, 43, 8, 5)
    put(crop(load("rod_side_w"), 2, 4), X - 1, 47, 2, 4)
    put(crop(load("knob_side_s"), 4, 4), X - 2, 51, 4, 4)
    put(crop(load("eye"), 2, 2), X - 3, 42, 2, 2)
    put(crop(load("eye"), 2, 2), X + 1, 42, 2, 2)
    put(crop(load("nose_front_s"), 2, 3), X - 1, 40, 2, 3)
    put(crop(load("barrel_side_s"), 16, 12), X - 8, 38, 16, 12)
    # spyglass pointing right from the right eye, fully out
    put(crop(load("spy_eye"), 4, 2), X + 3, 42, 4, 2)
    put(crop(load("spy_mid"), 6, 3), X + 7, 42.5, 6, 3)
    put(crop(load("spy_front"), 6, 4), X + 13, 43, 6, 4)
    put(crop(load("lens"), 4, 4), X + 19, 43, 1.5, 4)
    put(load("fx_spark"), X + 18.5, 46.5, 5, 5)
    # thought-bubble trail and the bubble
    put(load("dot_s"), X + 24, 47, 3, 3)
    put(load("dot_m"), X + 28, 51, 4.5, 4.5)
    bx, by = X + 48, 58  # bubble centre
    put(load("bubble_view"), bx - 16, by + 16, 32, 32)
    # feet on the far hills (row 19 of the view), magnified to 0.85 of its sprite
    put(load("creeper_far"), bx - 3.4, by - 3 + 15.3, 6.8, 15.3)
    put(load("bubble_ring"), bx - 16, by + 16, 32, 32)
    put(load("fx_alert"), X + 4, 58, 3, 7.5)
    return img


def outline(img, colour=(10, 26, 44, 255), r=1):
    a = img.split()[3]
    from PIL import ImageFilter
    grown = a.filter(ImageFilter.MaxFilter(2 * r + 1))
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(Image.new("RGBA", img.size, colour), (0, 0), grown)
    out.alpha_composite(img)
    return out


def main():
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else HERE / "review.png"
    names = sorted(p.stem for p in SPR.glob("*.png")
                   if not p.stem.startswith(COPIED) and not p.stem.startswith(("bg_", "banner_", "review")))
    cells = []
    for n in names:
        im = load(n)
        z = max(1, min(10, 160 // max(im.width, im.height)))
        cells.append((n, im.resize((im.width * z, im.height * z), Image.NEAREST)))
    cw, ch, cols = 180, 190, 8
    rows = (len(cells) + cols - 1) // cols
    mock = mockup(8)
    small = mock.resize((96 * mock.width // mock.height, 96), Image.LANCZOS)
    title = load("banner_title")
    tag = load("banner_tagline")
    banner = load("banner_bg").resize((192 * 8, 64 * 8), Image.NEAREST)
    banner.alpha_composite(title.resize((title.width * 8, title.height * 8), Image.NEAREST), (72, 150))
    banner.alpha_composite(tag.resize((tag.width * 6, tag.height * 6), Image.NEAREST), (74, 300))
    W = max(cols * cw, mock.width + 40 + small.width * 2 + 40, banner.width)
    H = rows * ch + mock.height + 60 + banner.height + 40
    sheet = Image.new("RGBA", (W, H), (20, 24, 34, 255))
    d = ImageDraw.Draw(sheet)
    for i, (n, im) in enumerate(cells):
        x, y = (i % cols) * cw, (i // cols) * ch
        sheet.alpha_composite(im, (x + (cw - im.width) // 2, y + 8))
        d.text((x + 6, y + ch - 20), n, fill=(220, 220, 220))
    y0 = rows * ch + 20
    sheet.alpha_composite(mock, (20, y0))
    sheet.alpha_composite(small, (mock.width + 60, y0))
    sheet.alpha_composite(small.resize((small.width * 2, small.height * 2), Image.NEAREST), (mock.width + 60, y0 + 120))
    d.text((mock.width + 60, y0 + 100), "96 px (and 2x of it)", fill=(220, 220, 220))
    sheet.alpha_composite(banner, (0, y0 + mock.height + 20))
    sheet.convert("RGB").save(out)
    print(out)


if __name__ == "__main__":
    main()
