"""Lint: licht vlak + de twee donkere vouwen, allemaal als curven."""
from PIL import Image
import json, math, sys

sys.path.insert(0, "/home/user/logo-work")
from lib import contour_van_masker, rdp, gladstrijken, fit_curve, curven_naar_pad, bezier_punt

BRON = "/home/user/family-stop-v2/image-search/google-family-link-app-icon-logo-2.png"
im = Image.open(BRON).convert("RGBA")
W, H = im.size
px = im.load()

GRIJS_L = (218, 220, 224)
GRIJS_D = (189, 193, 198)


def dicht(c, doel, tol):
    return c[3] > 40 and sum(abs(a - b) for a, b in zip(c[:3], doel)) < tol


# ------------------------------------------------------------ maskers
def masker_van(test, tol_licht=None):
    m = Image.new("L", (W, H), 0)
    p = m.load()
    for y in range(H):
        for x in range(W):
            if test(px[x, y]):
                p[x, y] = 255
    return m


alles = masker_van(lambda c: dicht(c, GRIJS_L, 30) or dicht(c, GRIJS_D, 25))
donker = masker_van(lambda c: dicht(c, GRIJS_D, 22) and not dicht(c, GRIJS_L, 12))

# ------------------------------------------------------------ componenten
def componenten(masker, min_oppervlak=40):
    W_, H_ = masker.size
    p = masker.load()
    gezien = [[False] * W_ for _ in range(H_)]
    uit = []
    for y0 in range(H_):
        for x0 in range(W_):
            if p[x0, y0] and not gezien[y0][x0]:
                stapel = [(x0, y0)]
                gezien[y0][x0] = True
                punten = []
                while stapel:
                    x, y = stapel.pop()
                    punten.append((x, y))
                    for dx in (-1, 0, 1):
                        for dy in (-1, 0, 1):
                            nx, ny = x + dx, y + dy
                            if 0 <= nx < W_ and 0 <= ny < H_ and p[nx, ny] and not gezien[ny][nx]:
                                gezien[ny][nx] = True
                                stapel.append((nx, ny))
                if len(punten) >= min_oppervlak:
                    uit.append(punten)
    return uit


delen = componenten(donker, 60)
print("donkere vouwen gevonden:", len(delen))
for i, deel in enumerate(delen):
    xs = [p[0] for p in deel]
    ys = [p[1] for p in deel]
    print("   deel %d: %d px, x %d..%d, y %d..%d" % (i, len(deel), min(xs), max(xs), min(ys), max(ys)))


def fit_masker(masker, tol_fit=0.35, venster=5, rdp_eps=0.28):
    contour = contour_van_masker(masker, F=4)
    links_idx = min(range(len(contour)), key=lambda i: contour[i][0])
    contour = contour[links_idx:] + contour[:links_idx]
    glad = gladstrijken(contour, venster=venster, rond=True)
    simpel = rdp(glad, rdp_eps)
    curven = fit_curve(simpel, tol_fit)
    # afwijking meten naar punten OP de curve
    op_curve = [bezier_punt(b, t / 8.0) for b in curven for t in range(9)]
    fout = max(min(math.hypot(p[0] - q[0], p[1] - q[1]) for q in op_curve) for p in contour[::4])
    return curven, fout, len(contour)


lint_curven, fout, n = fit_masker(alles)
print("\nlint: %d contourpunten -> %d curven  (max afwijking %.3f px)" % (n, len(lint_curven), fout))

vouw_curven = []
for i, deel in enumerate(delen):
    m = Image.new("L", (W, H), 0)
    mp = m.load()
    for x, y in deel:
        mp[x, y] = 255
    c, f, n = fit_masker(m, tol_fit=0.30, venster=4, rdp_eps=0.22)
    print("vouw %d: %d punten -> %d curven (max afwijking %.3f px)" % (i, n, len(c), f))
    vouw_curven.append(c)

json.dump({
    "lint": {"curven": lint_curven, "pad": curven_naar_pad(lint_curven) + " Z"},
    "vouwen": [{"curven": c, "pad": curven_naar_pad(c) + " Z"} for c in vouw_curven],
}, open("/home/user/logo-work/lint_klaar.json", "w"))
print("\nopgeslagen in lint_klaar.json")
