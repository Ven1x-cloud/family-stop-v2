"""Bouw de kite exact op: rechte randen + vier afgeronde hoeken met dezelfde straal."""
from PIL import Image
import json, math, sys

sys.path.insert(0, "/home/user/logo-work")
from lib import boog_naar_cubics, curven_naar_pad

BRON = "/home/user/family-stop-v2/image-search/google-family-link-app-icon-logo-2.png"
im = Image.open(BRON).convert("RGBA")
W, H = im.size
px = im.load()
alpha = [[px[x, y][3] / 255.0 for x in range(W)] for y in range(H)]


def rand_rechts(y):
    for x in range(W - 2, 0, -1):
        if alpha[y][x] >= 0.5 and alpha[y][x + 1] < 0.5:
            a, b = alpha[y][x + 1], alpha[y][x]
            return x + 1 - (0.5 - a) / (b - a)
    return None


def rand_links(y):
    for x in range(1, W):
        if alpha[y][x] >= 0.5 and alpha[y][x - 1] < 0.5:
            a, b = alpha[y][x - 1], alpha[y][x]
            return x - 1 + (0.5 - a) / (b - a)
    return None


assen = [(rand_links(y) + rand_rechts(y)) / 2 for y in range(40, 110)]
AS = sum(assen) / len(assen)

Y_TOP, Y_BOT = 12, 268
metingen = {y: rand_rechts(y) - AS for y in range(Y_TOP, Y_BOT + 1) if rand_rechts(y)}


def lijnfit(ps):
    n = len(ps)
    sx = sum(p[0] for p in ps); sy = sum(p[1] for p in ps)
    sxx = sum(p[0] * p[0] for p in ps); sxy = sum(p[0] * p[1] for p in ps)
    a = (n * sxy - sx * sy) / (n * sxx - sx * sx)
    b = (sy - a * sx) / n
    return a, b


boven = [(y, w) for y, w in metingen.items() if 30 <= y <= 100]
onder = [(y, w) for y, w in metingen.items() if 150 <= y <= 252]
a1, c1 = lijnfit(boven)
a2, c2 = lijnfit(onder)
n1 = math.sqrt(1 + a1 * a1)
n2 = math.sqrt(1 + a2 * a2)
print("bovenrand: w = %+0.6f*y %+0.4f  (max afwijking %.4f px)" %
      (a1, c1, max(abs(a1 * y + c1 - w) for y, w in boven)))
print("onderrand: w = %+0.6f*y %+0.4f  (max afwijking %.4f px)" %
      (a2, c2, max(abs(a2 * y + c2 - w) for y, w in onder)))

y_top_apex = -c1 / a1
y_bot_apex = -c2 / a2
y_hoek = (c2 - c1) / (a1 - a2)
w_hoek = a1 * y_hoek + c1
print("scherpe hoekpunten: top y=%.3f  onder y=%.3f  zijkant y=%.3f w=%.3f" %
      (y_top_apex, y_bot_apex, y_hoek, w_hoek))


def hoekpunt(w, y, r):
    """Geeft de ronde hoek: (midden_x, midden_y, raakpunt1, raakpunt2)."""
    pass


def geometrie(r):
    """Alle maten voor straal r. y-as loopt omlaag."""
    # de randen naar binnen opschuiven en snijden
    # bovenrand (binnenkant): w = a1*y + c1 - r*n1
    # onderrand (binnenkant): w = a2*y + c2 - r*n2
    yc_zij = ((c1 - r * n1) - (c2 - r * n2)) / (a2 - a1)
    wc_zij = a1 * yc_zij + c1 - r * n1
    yc_top = (r * n1 - c1) / a1
    yc_bot = (r * n2 - c2) / a2

    def voet(yc, wc, a, c):
        """Loodrechte projectie van het midden op de rand -> raakpunt."""
        F = a * yc - wc + c
        return (wc + F / (1 + a * a), yc - a * F / (1 + a * a))

    t_top = voet(yc_top, 0.0, a1, c1)
    t_boven = voet(yc_zij, wc_zij, a1, c1)
    t_onder = voet(yc_zij, wc_zij, a2, c2)
    t_bot = voet(yc_bot, 0.0, a2, c2)
    return {
        "r": r, "yc_top": yc_top, "yc_bot": yc_bot, "yc_zij": yc_zij, "wc_zij": wc_zij,
        "t_top": t_top, "t_boven": t_boven, "t_onder": t_onder, "t_bot": t_bot,
        "y_top": yc_top - r, "y_bot": yc_bot + r,
    }


def halve_breedte(y, g):
    r = g["r"]
    if y < g["y_top"] or y > g["y_bot"]:
        return None
    if y <= g["t_top"][1]:
        return math.sqrt(max(0.0, r * r - (y - g["yc_top"]) ** 2))
    if y <= g["t_boven"][1]:
        return a1 * y + c1
    if y <= g["t_onder"][1]:
        return g["wc_zij"] + math.sqrt(max(0.0, r * r - (y - g["yc_zij"]) ** 2))
    if y <= g["t_bot"][1]:
        return a2 * y + c2
    return math.sqrt(max(0.0, r * r - (y - g["yc_bot"]) ** 2))


def fouten_voor(r):
    g = geometrie(r)
    fouten = {y: abs(halve_breedte(y, g) - wm) for y, wm in metingen.items()
              if halve_breedte(y, g) is not None}
    ontbreekt = sum(1 for y in metingen if halve_breedte(y, g) is None)
    return fouten, ontbreekt


beste = None
rr = 12.0
while rr <= 26.0:
    fouten, ontbreekt = fouten_voor(rr)
    mx = max(fouten.values())
    gem = sum(fouten.values()) / len(fouten)
    score = mx + ontbreekt * 10
    if beste is None or score < beste[1]:
        beste = (rr, score, mx, gem)
    rr += 0.01
print("\nbeste straal: %.3f  max fout %.3f px  gemiddeld %.3f px" %
      (beste[0], beste[2], beste[3]))

R = beste[0]
g = geometrie(R)
fouten, _ = fouten_voor(R)
print("\nafwijking per gebied (px):")
for y in sorted(fouten):
    if y % 12 == 0:
        print("   y=%3d  %.3f" % (y, fouten[y]))
print("   grootste afwijking bij y=%d: %.3f" %
      (max(fouten, key=fouten.get), max(fouten.values())))

print("\nraakpunten (w, y):")
for naam in ("t_top", "t_boven", "t_onder", "t_bot"):
    print("   %-8s w=%8.3f  y=%8.3f" % (naam, g[naam][0], g[naam][1]))
print("controle stralen: %.5f %.5f %.5f %.5f" % (
    math.hypot(g["t_top"][0], g["t_top"][1] - g["yc_top"]),
    math.hypot(g["t_boven"][0] - g["wc_zij"], g["t_boven"][1] - g["yc_zij"]),
    math.hypot(g["t_onder"][0] - g["wc_zij"], g["t_onder"][1] - g["yc_zij"]),
    math.hypot(g["t_bot"][0], g["t_bot"][1] - g["yc_bot"])))
print("y_top=%.3f (gemeten %.3f)   y_bot=%.3f (gemeten %.3f)" %
      (g["y_top"], min(metingen), g["y_bot"], max(metingen)))

# ---------------------------------------------------------------- pad
a_top_start = -math.pi / 2
a_top_eind = math.atan2(g["t_top"][1] - g["yc_top"], g["t_top"][0])
a_zij_start = math.atan2(g["t_boven"][1] - g["yc_zij"], g["t_boven"][0] - g["wc_zij"])
a_zij_eind = math.atan2(g["t_onder"][1] - g["yc_zij"], g["t_onder"][0] - g["wc_zij"])
a_bot_start = math.atan2(g["t_bot"][1] - g["yc_bot"], g["t_bot"][0])
a_bot_eind = math.pi / 2
print("bogen (graden): top %.2f->%.2f  zij %.2f->%.2f  onder %.2f->%.2f" % (
    math.degrees(a_top_start), math.degrees(a_top_eind),
    math.degrees(a_zij_start), math.degrees(a_zij_eind),
    math.degrees(a_bot_start), math.degrees(a_bot_eind)))

rechts = []
rechts += boog_naar_cubics(0, g["yc_top"], R, a_top_start, a_top_eind)
rechts.append((g["t_top"], g["t_boven"], g["t_boven"], g["t_boven"]))
rechts += boog_naar_cubics(g["wc_zij"], g["yc_zij"], R, a_zij_start, a_zij_eind)
rechts.append((g["t_onder"], g["t_bot"], g["t_bot"], g["t_bot"]))
rechts += boog_naar_cubics(0, g["yc_bot"], R, a_bot_start, a_bot_eind)

links = []
for b in reversed(rechts):
    links.append(tuple((-p[0], p[1]) for p in (b[3], b[2], b[1], b[0])))

curven = rechts + links
pad = curven_naar_pad(curven)
print("\nkite-pad opgebouwd uit %d curven" % len(curven))

json.dump({"as": AS, "R": R, "curven": curven, "pad_lokaal": pad,
           "a1": a1, "c1": c1, "a2": a2, "c2": c2, "g": g,
           "top": min(metingen), "bot": max(metingen)},
          open("/home/user/logo-work/kite_klaar.json", "w"))
print("opgeslagen in kite_klaar.json")
