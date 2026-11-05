"""Generate the 16x16 cyan skill icons (textures/skill/<id>.png).

Each glyph is a shape test in a 12x12 box, supersampled and snapped to two tones so the pixels stay crisp.
Run from the repo root: python tools/gen_skill_icons.py
"""
import math
import os

from PIL import Image

OUT = os.path.join("src", "main", "resources", "assets", "kuiba", "textures", "skill")
SS = 8

FRAME = (196, 244, 255, 255)
BG = (12, 38, 50, 255)
BG_EDGE = (18, 56, 72, 255)
BRIGHT = (150, 240, 255, 255)
MID = (54, 170, 214, 255)


def seg(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def ring(x, y, cx, cy, r, w):
    return abs(math.hypot(x - cx, y - cy) - r) < w / 2


def disc(x, y, cx, cy, r):
    return math.hypot(x - cx, y - cy) < r


def hexagon(x, y, cx, cy, r):
    dx, dy = abs(x - cx), abs(y - cy)
    return dy <= r * 0.866 and dx * 0.866 + dy * 0.5 <= r * 0.866


# Each glyph returns 2 (bright), 1 (mid) or 0 for a point in [0, 12)^2.
def flash(x, y):
    for cx, tone in ((5.0, 1), (9.0, 2)):
        if abs(y - 6) < 5 and abs((x - cx) + abs(y - 6) * 0.9) < 0.9:
            return tone
    return 0


def strike(x, y):
    if disc(x, y, 6, 6, 1.9):
        return 2
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 4
        diagonal = k % 2 == 0
        length = 5.9 if diagonal else 3.4
        if seg(x, y, 6, 6, 6 + math.cos(a) * length, 6 + math.sin(a) * length) < (0.95 if diagonal else 0.6):
            return 2 if diagonal else 1
    return 0


def bolt(x, y):
    if disc(x, y, 6, 6, 2.6):
        return 2
    if ring(x, y, 6, 6, 4.8, 1.0):
        return 1
    return 0


def wave(x, y):
    if disc(x, y, 1.8, 6, 1.3):
        return 2
    a = math.degrees(math.atan2(y - 6, x - 0.5))
    if abs(a) < 55:
        for r, tone in ((4.0, 2), (7.0, 2), (10.0, 1)):
            if ring(x, y, 0.5, 6, r, 1.1):
                return tone
    return 0


def _shield(x, y, m):
    return 1.5 + m <= x <= 10.5 - m and 1.0 + m <= y <= 5.5 + (4.5 - abs(x - 6)) * 1.25 - m * 1.6


def guard(x, y):
    if _shield(x, y, 0) and not _shield(x, y, 1.1):
        return 2
    if _shield(x, y, 2.1):
        return 1
    return 0


def mend(x, y):
    if (abs(x - 6) < 1.5 and 1 < y < 11) or (abs(y - 6) < 1.5 and 1 < x < 11):
        return 2
    return 0


def edge(x, y):
    if seg(x, y, 3.2, 8.8, 10.4, 1.6) < 0.95:
        return 2
    if seg(x, y, 2.0, 7.0, 5.0, 10.0) < 0.8:
        return 1
    if seg(x, y, 1.4, 10.6, 3.0, 9.0) < 0.8:
        return 1
    return 0


def bind(x, y):
    if ring(x, y, 4.4, 6, 3.0, 1.0):
        return 2
    if ring(x, y, 7.6, 6, 3.0, 1.0):
        return 1
    return 0


def barrier(x, y):
    if hexagon(x, y, 6, 6, 5.6) and not hexagon(x, y, 6, 6, 4.4):
        return 2
    if hexagon(x, y, 6, 6, 2.6) and not hexagon(x, y, 6, 6, 1.4):
        return 1
    return 0


GLYPHS = {
    "pulse_flash": flash,
    "pulse_strike": strike,
    "pulse_bolt": bolt,
    "pulse_wave": wave,
    "mai_guard": guard,
    "mai_mend": mend,
    "mai_edge": edge,
    "mai_bind": bind,
    "mai_barrier": barrier,
}


def render(fn):
    img = Image.new("RGBA", (16, 16), FRAME)
    px = img.load()
    for j in range(1, 15):
        for i in range(1, 15):
            px[i, j] = BG_EDGE if i in (1, 14) or j in (1, 14) else BG
    for j in range(12):
        for i in range(12):
            hits = [0, 0, 0]
            for sj in range(SS):
                for si in range(SS):
                    hits[fn(i + (si + 0.5) / SS, j + (sj + 0.5) / SS)] += 1
            n = SS * SS
            if hits[2] / n >= 0.45:
                px[i + 2, j + 2] = BRIGHT
            elif (hits[1] + hits[2]) / n >= 0.40:
                px[i + 2, j + 2] = MID
    return img


if __name__ == "__main__":
    for name, fn in GLYPHS.items():
        render(fn).save(os.path.join(OUT, name + ".png"))
        print("wrote", name)
