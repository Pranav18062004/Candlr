"""Render Candlr's own vector mark into Play assets. Requires Pillow, not an image service."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store" / "graphics"
OUT.mkdir(parents=True, exist_ok=True)
SCALE = 3
PAPER, INK, OLIVE, LINE = "#F2F0E3", "#2E2E2E", "#62694B", "#CECEBB"


def font(size, serif=False):
    candidates = (["C:/Windows/Fonts/georgia.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf"] if serif else
                  ["C:/Windows/Fonts/segoeui.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"])
    return ImageFont.truetype(next(path for path in candidates if Path(path).exists()), size * SCALE)


def mark(draw, x, y, size, fill):
    def pt(p):
        return ((x + p[0] * size / 108) * SCALE, (y + p[1] * size / 108) * SCALE)
    def curve(a, b, c, d):
        return [pt(tuple((1-t)**3*a[k] + 3*(1-t)**2*t*b[k] + 3*(1-t)*t*t*c[k] + t**3*d[k] for k in (0, 1))) for t in [i / 60 for i in range(61)]]
    flame = curve((54,24), (42,37), (46,44), (54,44)) + curve((54,44), (62,44), (65,37), (54,24))
    draw.polygon(flame, fill=fill)
    draw.polygon([pt(p) for p in [(43,49),(65,49),(65,77),(54,71),(43,77)]], fill=fill)


icon = Image.new("RGB", (512*SCALE, 512*SCALE), PAPER)
mark(ImageDraw.Draw(icon), -60, -60, 632, OLIVE)
icon.resize((512,512), Image.Resampling.LANCZOS).convert("RGBA").save(OUT / "icon-512.png")

graphic = Image.new("RGB", (1024*SCALE,500*SCALE), PAPER)
d = ImageDraw.Draw(graphic)
d.text((72*SCALE,111*SCALE), "Candlr", font=font(83, True), fill=INK)
d.text((77*SCALE,230*SCALE), "A little book of your people.", font=font(26), fill=OLIVE)
d.text((77*SCALE,297*SCALE), "Birthdays, kept close.", font=font(19), fill=INK)
# A quiet book silhouette uses the same candle/bookmark mark as the Android launcher.
d.rounded_rectangle((710*SCALE,85*SCALE,920*SCALE,409*SCALE), radius=8*SCALE, fill=LINE)
d.rounded_rectangle((698*SCALE,77*SCALE,909*SCALE,397*SCALE), radius=8*SCALE, fill="#E1E4D3")
d.line((719*SCALE,80*SCALE,719*SCALE,394*SCALE), fill=LINE, width=2*SCALE)
mark(d, 639, 73, 335, OLIVE)
graphic.resize((1024,500), Image.Resampling.LANCZOS).save(OUT / "feature-1024x500.png")

assert len((ROOT / "store/listing/en-US/title.txt").read_text().strip()) <= 30
assert len((ROOT / "store/listing/en-US/short-description.txt").read_text().strip()) <= 80
assert len((ROOT / "store/listing/en-US/full-description.txt").read_text().strip()) <= 4000
print("Generated 512×512 icon and 1024×500 feature graphic; listing lengths verified.")
