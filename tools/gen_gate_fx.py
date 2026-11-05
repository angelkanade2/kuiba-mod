"""Generate the 脉门 ring textures (gate_ring / gate_core and their additive glows).

Radii are normalised to the quad half-size (r = 1 at the quad edge).
Run from the repo root: python tools/gen_gate_fx.py
"""
import math
import os

from PIL import Image

SIZE = 128
SS = 4  # supersamples per axis
OUT = os.path.join("src", "main", "resources", "assets", "kuiba", "textures", "fx")

WHITE = (255, 255, 255)
RIM = (232, 246, 255)
CYAN = (92, 200, 240)


def smooth(edge0, edge1, x):
    t = min(1.0, max(0.0, (x - edge0) / (edge1 - edge0)))
    return t * t * (3.0 - 2.0 * t)


def band(r, inner, outer, soft):
    return smooth(inner - soft, inner + soft, r) * (1.0 - smooth(outer - soft, outer + soft, r))


def gauss(r, center, width):
    d = (r - center) / width
    return math.exp(-d * d)


def over(dst, color, a):
    """Premultiplied 'over'. dst is [r, g, b, a] premultiplied."""
    a = min(1.0, max(0.0, a))
    k = 1.0 - a
    dst[0] = color[0] / 255.0 * a + dst[0] * k
    dst[1] = color[1] / 255.0 * a + dst[1] * k
    dst[2] = color[2] / 255.0 * a + dst[2] * k
    dst[3] = a + dst[3] * k


def ring_px(x, y):
    # Large ring that sits on the skin.
    r = math.hypot(x, y)
    c = [0.0, 0.0, 0.0, 0.0]
    over(c, CYAN, 0.40 * gauss(r, 0.86, 0.06))
    over(c, RIM, 0.95 * band(r, 0.835, 0.885, 0.008))
    return c


def core_px(x, y):
    # Small ring floated off the skin; thicker line so it reads at a third of the size.
    r = math.hypot(x, y)
    c = [0.0, 0.0, 0.0, 0.0]
    over(c, CYAN, 0.45 * gauss(r, 0.74, 0.12))
    over(c, RIM, 0.95 * band(r, 0.64, 0.84, 0.02))
    return c


def orb_px(x, y):
    # 脉弹 core: white-hot centre fading to cyan at the rim.
    r = math.hypot(x, y)
    c = [0.0, 0.0, 0.0, 0.0]
    over(c, CYAN, 0.9 * (1.0 - smooth(0.55, 0.95, r)))
    over(c, RIM, 1.0 - smooth(0.25, 0.6, r))
    over(c, WHITE, 1.0 - smooth(0.05, 0.3, r))
    return c


def bloom(center, width, strength):
    """Additive glow for RenderType.eyes: brightness lives in RGB, black where empty."""
    def px(x, y):
        r = math.hypot(x, y)
        k = strength * gauss(r, center, width) * (1.0 - smooth(0.88, 0.98, r))
        return [CYAN[0] / 255.0 * k, CYAN[1] / 255.0 * k, CYAN[2] / 255.0 * k, 1.0]
    return px


def render(fn, name):
    img = Image.new("RGBA", (SIZE, SIZE))
    px = img.load()
    n = SS * SS
    for j in range(SIZE):
        for i in range(SIZE):
            acc = [0.0, 0.0, 0.0, 0.0]
            for sj in range(SS):
                for si in range(SS):
                    x = ((i + (si + 0.5) / SS) / SIZE) * 2.0 - 1.0
                    y = ((j + (sj + 0.5) / SS) / SIZE) * 2.0 - 1.0
                    c = fn(x, y)
                    for k in range(4):
                        acc[k] += c[k]
            a = acc[3] / n
            if a < 8 / 255:
                px[i, j] = (0, 0, 0, 0)
                continue
            px[i, j] = tuple(min(255, round(acc[k] / n / a * 255)) for k in range(3)) + (round(a * 255),)
    img.save(os.path.join(OUT, name))
    print("wrote", name)


if __name__ == "__main__":
    render(ring_px, "gate_ring.png")
    render(core_px, "gate_core.png")
    # Line radius / glow quad scale, matching MaiGateLayer (ring at 0.86 on a 1.1x quad, core 0.74 on 1.6x).
    render(bloom(0.86 / 1.1, 0.10, 1.0), "gate_ring_glow.png")
    render(bloom(0.74 / 1.6, 0.16, 1.0), "gate_core_glow.png")
    # 脉弹: orb on a 1x quad, its bloom on a 2.6x quad (MaiBoltRenderer).
    render(orb_px, "mai_orb.png")
    render(bloom(0.0, 0.42, 1.0), "mai_orb_glow.png")
