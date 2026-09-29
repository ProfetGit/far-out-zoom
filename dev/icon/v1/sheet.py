#!/usr/bin/env python3
"""Review sheets for the Crow's Nest icon renders: every frame (or a range) on the ocean background with the dark
outline, as a grid, plus a 96 px strip.

  python3 dev/icon/sheet.py <frames_dir> <out.png> [--every N] [--cell 200] [--from F] [--to F]
"""
import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

BG, SHADOW, INK = (35, 80, 126), (26, 61, 98), (10, 26, 44)


def compose(path, cell):
    import numpy as np
    im = np.array(Image.open(path).convert("RGBA"))
    key = (im[..., 3] > 0) & (im[..., 0] > 240) & (im[..., 1] < 20) & (im[..., 2] > 240)
    art = im.copy()
    art[key] = 0
    out = np.empty(im.shape[:2] + (3,), np.uint8)
    out[:] = BG
    out[key] = SHADOW
    solid = Image.fromarray(((art[..., 3] > 0) * 255).astype(np.uint8))
    grown = np.array(solid.filter(ImageFilter.MaxFilter(max(3, im.shape[1] // 200 * 2 + 1)))) > 0
    out[grown] = INK
    a = art[..., 3] > 0
    out[a] = art[..., :3][a]
    return Image.fromarray(out).resize((cell, cell), Image.NEAREST)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("frames")
    ap.add_argument("out")
    ap.add_argument("--every", type=int, default=1)
    ap.add_argument("--cell", type=int, default=200)
    ap.add_argument("--cols", type=int, default=10)
    ap.add_argument("--from", dest="f0", type=int, default=0)
    ap.add_argument("--to", dest="f1", type=int, default=999)
    a = ap.parse_args()
    files = sorted(Path(a.frames).glob("frame_*.png"))
    files = [f for f in files if a.f0 <= int(f.stem[6:]) <= a.f1][::a.every]
    rows = (len(files) + a.cols - 1) // a.cols
    sheet = Image.new("RGB", (a.cols * a.cell, rows * a.cell + 110), (20, 24, 34))
    d = ImageDraw.Draw(sheet)
    for i, f in enumerate(files):
        c = compose(f, a.cell)
        sheet.paste(c, ((i % a.cols) * a.cell, (i // a.cols) * a.cell))
        d.text(((i % a.cols) * a.cell + 4, (i // a.cols) * a.cell + 4), f.stem[6:], fill=(255, 255, 120))
    for i, f in enumerate(files[:: max(1, len(files) // 12)][:12]):
        sheet.paste(compose(f, 96), (i * 100, rows * a.cell + 7))
    sheet.save(a.out)
    print(a.out, len(files), "frames")


if __name__ == "__main__":
    main()
