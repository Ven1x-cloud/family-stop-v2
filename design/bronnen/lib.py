"""Hulpmiddelen: Bezier-fitting, contouren uitlezen en paden schrijven."""
import math
from PIL import Image


# ============================================================ Bezier-fitting

def chord_parameterize(punten):
    u = [0.0]
    for i in range(1, len(punten)):
        d = math.hypot(punten[i][0] - punten[i - 1][0], punten[i][1] - punten[i - 1][1])
        u.append(u[-1] + d)
    totaal = u[-1] or 1.0
    return [x / totaal for x in u]


def bezier_punt(b, t):
    mt = 1 - t
    return (mt ** 3 * b[0][0] + 3 * mt ** 2 * t * b[1][0] + 3 * mt * t ** 2 * b[2][0] + t ** 3 * b[3][0],
            mt ** 3 * b[0][1] + 3 * mt ** 2 * t * b[1][1] + 3 * mt * t ** 2 * b[2][1] + t ** 3 * b[3][1])


def genereer_bezier(punten, u, t1, t2):
    eerste, laatste = punten[0], punten[-1]
    a = [[0.0, 0.0], [0.0, 0.0]]
    x = [0.0, 0.0]
    for i, p in enumerate(punten):
        t = u[i]
        mt = 1 - t
        b1 = 3 * mt ** 2 * t
        b2 = 3 * mt * t ** 2
        a[0][0] += b1 * b1
        a[0][1] += b1 * b2
        a[1][0] = a[0][1]
        a[1][1] += b2 * b2
        tmp = (p[0] - (mt ** 3 + b1) * eerste[0] - (t ** 3 + b2) * laatste[0],
               p[1] - (mt ** 3 + b1) * eerste[1] - (t ** 3 + b2) * laatste[1])
        x[0] += b1 * (tmp[0] * t1[0] + tmp[1] * t1[1])
        x[1] += b2 * (tmp[0] * t2[0] + tmp[1] * t2[1])

    det = a[0][0] * a[1][1] - a[1][0] * a[0][1]
    det_c1 = x[0] * a[1][1] - x[1] * a[0][1]
    det_c2 = a[0][0] * x[1] - a[1][0] * x[0]

    if abs(det) < 1e-12:
        afstand = math.hypot(laatste[0] - eerste[0], laatste[1] - eerste[1]) / 3.0
        c1 = [eerste[0] + t1[0] * afstand, eerste[1] + t1[1] * afstand]
        c2 = [laatste[0] + t2[0] * afstand, laatste[1] + t2[1] * afstand]
    else:
        c1 = [eerste[0] + t1[0] * (det_c1 / det), eerste[1] + t1[1] * (det_c1 / det)]
        c2 = [laatste[0] + t2[0] * (det_c2 / det), laatste[1] + t2[1] * (det_c2 / det)]
    return [eerste, c1, c2, laatste]


def nieuwe_raaklijnen(punten, u, b):
    t1 = [0.0, 0.0]
    t2 = [0.0, 0.0]
    for i, p in enumerate(punten):
        t = u[i]
        mt = 1 - t
        q1 = 3 * mt ** 2 * t
        q2 = 3 * mt * t ** 2
        basis = (mt ** 3 * b[0][0] + q1 * b[1][0] + q2 * b[2][0] + t ** 3 * b[3][0],
                 mt ** 3 * b[0][1] + q1 * b[1][1] + q2 * b[2][1] + t ** 3 * b[3][1])
        t1[0] += q1 * (p[0] - basis[0])
        t1[1] += q1 * (p[1] - basis[1])
        t2[0] += q2 * (p[0] - basis[0])
        t2[1] += q2 * (p[1] - basis[1])
    for v in (t1, t2):
        lengte = math.hypot(v[0], v[1])
        if lengte > 1e-9:
            v[0] /= lengte
            v[1] /= lengte
    return t1, t2


def raaklijn_uiteinde(punten, aan_begin):
    n = 1
    while n < len(punten) - 1:
        p = punten[n] if aan_begin else punten[-1 - n]
        q = punten[0] if aan_begin else punten[-1]
        v = (p[0] - q[0], p[1] - q[1])
        lengte = math.hypot(v[0], v[1])
        if lengte > 1e-9:
            return (v[0] / lengte, v[1] / lengte)
        n += 1
    return (1.0, 0.0)


def fit_curve(punten, fout, diepte=0):
    if len(punten) < 2:
        return []
    if len(punten) == 2:
        p, q = punten
        return [(p, (p[0] + (q[0] - p[0]) / 3, p[1] + (q[1] - p[1]) / 3),
                 (p[0] + 2 * (q[0] - p[0]) / 3, p[1] + 2 * (q[1] - p[1]) / 3), q)]
    u = chord_parameterize(punten)
    b = genereer_bezier(punten, u, raaklijn_uiteinde(punten, True), raaklijn_uiteinde(punten, False))
    afwijking = max(math.hypot(bezier_punt(b, u[i])[0] - p[0], bezier_punt(b, u[i])[1] - p[1])
                    for i, p in enumerate(punten))
    if afwijking < fout:
        return [b]

    bn = genereer_bezier(punten, u, *nieuwe_raaklijnen(punten, u, b))
    afwijking_n = max(math.hypot(bezier_punt(bn, u[i])[0] - p[0], bezier_punt(bn, u[i])[1] - p[1])
                      for i, p in enumerate(punten))
    if afwijking_n < afwijking:
        b, afwijking = bn, afwijking_n
        if afwijking < fout:
            return [b]

    if diepte > 14:
        return [b]

    idx, grootste = len(punten) // 2, -1.0
    for i, p in enumerate(punten):
        d = math.hypot(bezier_punt(b, u[i])[0] - p[0], bezier_punt(b, u[i])[1] - p[1])
        if d > grootste:
            grootste, idx = d, i
    if idx <= 0 or idx >= len(punten) - 1:
        idx = len(punten) // 2
    return fit_curve(punten[:idx + 1], fout, diepte + 1) + fit_curve(punten[idx:], fout, diepte + 1)


def rdp(punten, eps):
    if len(punten) < 3:
        return punten

    def afstand(p, a, b):
        if a == b:
            return math.hypot(p[0] - a[0], p[1] - a[1])
        t = ((p[0] - a[0]) * (b[0] - a[0]) + (p[1] - a[1]) * (b[1] - a[1])) / \
            ((b[0] - a[0]) ** 2 + (b[1] - a[1]) ** 2)
        t = max(0.0, min(1.0, t))
        return math.hypot(p[0] - (a[0] + t * (b[0] - a[0])), p[1] - (a[1] + t * (b[1] - a[1])))

    dmax, index = 0, 0
    for i in range(1, len(punten) - 1):
        d = afstand(punten[i], punten[0], punten[-1])
        if d > dmax:
            dmax, index = d, i
    if dmax > eps:
        return rdp(punten[:index + 1], eps)[:-1] + rdp(punten[index:], eps)
    return [punten[0], punten[-1]]


def gladstrijken(punten, venster=5, rond=True):
    """Lichte moving-average om pixelruis weg te halen voor het fitten."""
    n = len(punten)
    if n < venster * 2:
        return punten
    uit = []
    for i in range(n):
        sx = sy = 0.0
        aantal = 0
        for k in range(-venster, venster + 1):
            j = (i + k) % n if rond else i + k
            if 0 <= j < n:
                sx += punten[j][0]
                sy += punten[j][1]
                aantal += 1
        uit.append((sx / aantal, sy / aantal))
    return uit


# ============================================================ contour uitlezen

def contour_van_masker(masker, F=4):
    W, H = masker.size
    groot = masker.resize((W * F, H * F), Image.LANCZOS)
    bw, bh = groot.size
    g = groot.load()

    def aan(x, y):
        return 0 <= x < bw and 0 <= y < bh and g[x, y] > 128

    start = None
    for y in range(bh):
        for x in range(bw):
            if aan(x, y):
                start = (x, y)
                break
        if start:
            break

    buur = [(1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0), (-1, -1), (0, -1), (1, -1)]
    contour = [start]
    vorige, huidig = (start[0] - 1, start[1]), start
    for _ in range(600000):
        idx = buur.index((vorige[0] - huidig[0], vorige[1] - huidig[1]))
        gevonden = None
        for k in range(1, 9):
            kand = buur[(idx + k) % 8]
            nx, ny = huidig[0] + kand[0], huidig[1] + kand[1]
            if aan(nx, ny):
                gevonden = (nx, ny)
                vorige = (huidig[0] + buur[(idx + k - 1) % 8][0],
                          huidig[1] + buur[(idx + k - 1) % 8][1])
                break
        if not gevonden:
            break
        huidig = gevonden
        contour.append(huidig)
        if huidig == start and len(contour) > 10:
            break
    return [(x / F, y / F) for x, y in contour]


# ============================================================ paden schrijven

def curven_naar_pad(curven, decimalen=2):
    if not curven:
        return ""
    fmt = "%." + str(decimalen) + "f"
    delen = ["M " + (fmt + " " + fmt) % curven[0][0]]
    vorige = curven[0][0]
    for b in curven:
        c1, c2, eind = b[1], b[2], b[3]
        if abs(c1[0] - vorige[0]) < 1e-9 and abs(c1[1] - vorige[1]) < 1e-9 and \
           abs(c2[0] - eind[0]) < 1e-9 and abs(c2[1] - eind[1]) < 1e-9:
            delen.append("L " + (fmt + " " + fmt) % eind)
        else:
            delen.append("C " + " ".join(fmt % v for v in
                                         (c1[0], c1[1], c2[0], c2[1], eind[0], eind[1])))
        vorige = eind
    return " ".join(delen)


def boog_naar_cubics(cx, cy, r, hoek_van, hoek_tot, stappen=None):
    """Zet een cirkelboog om in cubic Beziers. Hoeken in radialen."""
    delta = hoek_tot - hoek_van
    if stappen is None:
        stappen = max(1, int(math.ceil(abs(delta) / (math.pi / 4))))
    uit = []
    stap = delta / stappen
    k = 4.0 / 3.0 * math.tan(stap / 4.0)
    for i in range(stappen):
        a0 = hoek_van + i * stap
        a1 = a0 + stap
        p0 = (cx + r * math.cos(a0), cy + r * math.sin(a0))
        p3 = (cx + r * math.cos(a1), cy + r * math.sin(a1))
        c1 = (p0[0] - k * r * math.sin(a0), p0[1] + k * r * math.cos(a0))
        c2 = (p3[0] + k * r * math.sin(a1), p3[1] - k * r * math.cos(a1))
        uit.append((p0, c1, c2, p3))
    return uit
