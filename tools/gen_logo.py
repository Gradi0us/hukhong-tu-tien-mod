"""Generate the 256x256 pixel-art logo for mod hukhong (Hu Khong Tu Tien Luc).

Renders a 64x64 void swirl on a purple-black background, then upscales with
nearest-neighbour so every logical pixel becomes a 4x4 block.
"""
import math
import os
import random

from PIL import Image

GRID = 64
SCALE = 4
OUT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "..", "src", "main", "resources", "logo.png",
)

PALETTE = [
    (6, 0, 12),      # deep void
    (18, 4, 32),     # dark violet
    (34, 10, 60),    # violet
    (62, 22, 104),   # purple
    (100, 46, 160),  # bright purple
    (150, 90, 220),  # lavender
    (210, 180, 255), # highlight
]


def color_for(t):
    """Map t in [0, 1] to a palette entry."""
    idx = min(int(t * len(PALETTE)), len(PALETTE) - 1)
    return PALETTE[max(0, idx)]


def render():
    rng = random.Random(260)
    img = Image.new("RGB", (GRID, GRID), PALETTE[0])
    px = img.load()
    c = (GRID - 1) / 2.0
    stars = set()
    while len(stars) < 60:
        x, y = rng.randrange(GRID), rng.randrange(GRID)
        if (x - c) ** 2 + (y - c) ** 2 > 18 ** 2:
            stars.add((x, y))

    for y in range(GRID):
        for x in range(GRID):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            theta = math.atan2(dy, dx)
            if r > 1.0:
                px[x, y] = PALETTE[0]
                continue
            # Spiral arms: phase grows with log radius -> swirl effect.
            arm = math.sin(3 * theta - 9 * math.log(r + 0.08))
            falloff = 1.0 - r  # brighter near the centre ring, fades outward
            t = 0.35 + 0.35 * (arm * 0.5 + 0.5) * falloff + 0.2 * (1 - r)
            if r < 0.22:
                # Event horizon: near black with a faint rim.
                t = 0.0 if r < 0.16 else 0.45
            col = color_for(max(0.0, min(0.999, t)))
            px[x, y] = col

    for (x, y) in stars:
        px[x, y] = PALETTE[-1] if rng.random() < 0.4 else PALETTE[5]

    return img


def main():
    small = render()
    big = small.resize((GRID * SCALE, GRID * SCALE), Image.NEAREST)
    out = os.path.normpath(OUT)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    big.save(out, format="PNG")
    print(f"wrote {out} ({big.size[0]}x{big.size[1]})")


if __name__ == "__main__":
    main()
