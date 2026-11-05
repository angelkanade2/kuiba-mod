import shutil
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
GEN = Path(r"C:\Users\Administrator\.cursor\projects\d-cursor-projects-Minecraft-mods-kuiba\assets")
SRC = ROOT / "emblems" / "source"
OUT = ROOT / "emblems" / "redrawn"

COLS = [("beast", "兽族"), ("dragon", "龙族"), ("wing", "翼族"), ("hui", "辉妖"), ("wu", "雾妖"),
        ("ta", "獭妖"), ("kisika", "基斯卡"), ("motuo", "默拓"), ("saku", "萨库"), ("gelemohe", "格勒莫赫")]
ROWS = [("king", "王"), ("general", "将"), ("knight", "侠"), ("civilian", "民")]
UNKNOWN = {"kisika", "motuo"}

CELL, PAD, HEAD = 200, 12, 60
font = ImageFont.truetype("msyh.ttc", 28)
big = ImageFont.truetype("msyh.ttc", 56)

OUT.mkdir(parents=True, exist_ok=True)
w = HEAD + len(COLS) * (CELL + PAD) + PAD
h = HEAD + len(ROWS) * (CELL + PAD) + PAD
grid = Image.new("RGB", (w, h), (24, 24, 26))
d = ImageDraw.Draw(grid)

for ci, (c, cname) in enumerate(COLS):
    x = HEAD + PAD + ci * (CELL + PAD)
    d.text((x + CELL / 2, HEAD / 2), cname, font=font, fill=(220, 220, 220), anchor="mm")
    for ri, (r, rname) in enumerate(ROWS):
        y = HEAD + PAD + ri * (CELL + PAD)
        if ci == 0:
            d.text((HEAD / 2 + 4, y + CELL / 2), rname, font=big, fill=(230, 230, 230), anchor="mm")
        d.rounded_rectangle((x, y, x + CELL, y + CELL), 10, fill=(107, 107, 107))
        if c in UNKNOWN:
            d.text((x + CELL / 2, y + CELL / 2), "?", font=big, fill=(235, 235, 235), anchor="mm")
            continue
        gen = GEN / f"{c}_{r}.jpg"
        if gen.exists():
            shutil.copy(gen, OUT / gen.name)
            icon = Image.open(gen).convert("RGB")
            icon = icon.crop((40, 20, icon.width - 40, icon.height - 60))
        else:
            src = Image.open(SRC / f"{c}_{r}.png").convert("RGB")
            icon = src.crop((24, 24, src.width - 24, src.height - 24))
        icon.thumbnail((CELL - 16, CELL - 16), Image.LANCZOS)
        grid.paste(icon, (x + (CELL - icon.width) // 2, y + (CELL - icon.height) // 2))

grid.save(ROOT / "emblems" / "emblems_grid.png")
print("saved", grid.size, len(list(OUT.glob("*.jpg"))), "redrawn")
