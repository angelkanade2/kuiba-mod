from pathlib import Path
from PIL import Image

SRC = Path(r"C:\Users\Administrator\.cursor\projects\d-cursor-projects-Minecraft-mods-kuiba\assets\c__Users_Administrator_AppData_Roaming_Cursor_User_workspaceStorage_1c66c68c7a9d1497590c047441c8b298_images_image-9140a1be-4ff8-43f3-a3f6-f6132dd70011.png")
OUT = Path(__file__).resolve().parent.parent / "emblems" / "source"

COLS = {
    "beast": (62, 150), "dragon": (158, 248), "wing": (256, 346),
    "hui": (356, 446), "wu": (456, 544), "ta": (554, 642),
    "saku": (838, 928), "gelemohe": (934, 1022),
}
ROWS = {"king": (62, 172), "general": (184, 296), "knight": (308, 418), "civilian": (428, 540)}

OUT.mkdir(parents=True, exist_ok=True)
img = Image.open(SRC).convert("RGB")
for c, (x0, x1) in COLS.items():
    for r, (y0, y1) in ROWS.items():
        img.crop((x0, y0, x1, y1)).resize(((x1 - x0) * 4, (y1 - y0) * 4), Image.LANCZOS).save(OUT / f"{c}_{r}.png")
print("done", img.size)
