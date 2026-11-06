"""
A quick look at the rig without a server: the item models posed exactly as Core's RigPose poses them (same matrices),
drawn flat-shaded from a few sides with each face's average texture colour. python3 preview.py out.png [ticks]
"""
import json
import math
import os
import sys
import zipfile

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "..", "src", "main", "resources", "de", "raindancer", "modules", "pets")
CLIENT = os.path.expanduser("~/.cache/rains-e2e/minecraft-client-26.3.jar")


def texture():
    with zipfile.ZipFile(CLIENT) as jar, jar.open("assets/minecraft/textures/entity/enderdragon/dragon.png") as f:
        return Image.open(f).convert("RGBA")


def T(x, y, z):
    m = np.eye(4); m[:3, 3] = [x, y, z]; return m


def S(x, y=None, z=None):
    y = x if y is None else y; z = x if z is None else z
    return np.diag([x, y, z, 1.0])


def RX(a):
    c, s = math.cos(a), math.sin(a); return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]])


def RY(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]])


def RZ(a):
    c, s = math.cos(a), math.sin(a); return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]])


def pose(rig, ticks, flying=True, biting=False):
    """Core's RigPose, line for line."""
    off = rig.get("offset", [0, 0, 0])
    root = T(off[0], 0, off[2]) @ S(-1, -1, 1) @ T(0, off[1], 0)
    flap = ticks * (0.05 if flying else 0.025) * math.pi * 2
    b = math.sin(flap - 1) + 1
    bounce = (b * b + b * 2) * 0.05
    joints, shown = [], []
    names = [p["name"] for p in rig["parts"]]
    neck = tail = None
    wave = 0.0
    for p in rig["parts"]:
        parent = names.index(p["parent"]) if p["parent"] else -1
        j = joints[parent] if parent >= 0 else root
        at = list(p["pivot"]); turn = list(p["rotation"])
        m = p.get("motion")
        if m:
            side = m.get("side", 1) or 1
            k = m["kind"]
            if k == "root":
                at[1] += (bounce - 2) * 16; turn[0] += math.radians(bounce * 2)
            elif k == "wing":
                turn[0] += 0.125 - math.cos(flap) * 0.2; turn[1] += -0.25 * side
                turn[2] += -(math.sin(flap) + 0.125) * 0.8 * side
            elif k == "wing_tip":
                turn[2] += (math.sin(flap + 2) + 0.5) * 0.75 * side
            elif k == "jaw":
                turn[0] += 0.75 if biting else (math.sin(flap) + 1) * 0.2
            elif k == "leg":
                turn[0] += bounce * 0.1 * side
            elif k == "neck":
                if neck is None:
                    neck = list(at)
                turn[0] += math.cos(m.get("index", 0) * 0.45 + flap) * 0.15
                at = list(neck)
                neck[1] += math.sin(turn[0]) * 10
                neck[2] -= math.cos(turn[1]) * math.cos(turn[0]) * 10
                neck[0] -= math.sin(turn[1]) * math.cos(turn[0]) * 10
            elif k == "head":
                if neck is not None:
                    at = list(neck)
            elif k == "tail":
                if tail is None:
                    tail = list(at)
                wave += math.sin(m.get("index", 0) * 0.45 + flap) * 0.05
                turn[0] += wave; turn[1] += math.pi
                at = list(tail)
                tail[1] += math.sin(turn[0]) * 10
                tail[2] -= math.cos(turn[1]) * math.cos(turn[0]) * 10
                tail[0] -= math.sin(turn[1]) * math.cos(turn[0]) * 10
        j = j @ T(*(v / 16 for v in at)) @ RZ(turn[2]) @ RY(turn[1]) @ RX(turn[0])
        joints.append(j)
        shown.append(j @ S(1 / p["modelScale"]))
    return shown


def element_quads(e):
    (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
    corners = {
        "down": [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)],
        "up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
        "north": [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)],
        "south": [(x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)],
        "west": [(x1, y1, z1), (x1, y1, z2), (x1, y2, z2), (x1, y2, z1)],
        "east": [(x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1)],
    }
    rot = e.get("rotation")
    m = np.eye(4)
    if rot:
        o = rot["origin"]; a = math.radians(rot["angle"])
        r = {"x": RX, "y": RY, "z": RZ}[rot["axis"]](a)
        m = T(*o) @ r @ T(-o[0], -o[1], -o[2])
    out = []
    for name, face in e["faces"].items():
        pts = [m @ np.array([*c, 1.0]) for c in corners[name]]
        out.append((name, face["uv"], [((p[0] - 8) / 16, (p[1] - 8) / 16, (p[2] - 8) / 16) for p in pts]))
    return out


def colour(tex, uv):
    u1, v1, u2, v2 = [x * 16 for x in uv]
    box = (int(min(u1, u2)), int(min(v1, v2)), max(int(min(u1, u2)) + 1, int(max(u1, u2))),
           max(int(min(v1, v2)) + 1, int(max(v1, v2))))
    region = np.array(tex.crop(box)).reshape(-1, 4)
    solid = region[region[:, 3] > 128]
    if len(solid) == 0:
        return None
    return tuple(int(c) for c in solid[:, :3].mean(axis=0))


def render(rig, tex, yaw, pitch, ticks, size=420):
    poses = pose(rig, ticks)
    view = RX(math.radians(pitch)) @ RY(math.radians(yaw))
    polys = []
    for part, m in zip(rig["parts"], poses):
        if not part["model"]:
            continue
        ns, key = part["model"].split(":")
        with open(os.path.join(RES, "pack", "assets", ns, "models", "item", key + ".json")) as f:
            model = json.load(f)
        for e in model["elements"]:
            for name, uv, pts in element_quads(e):
                c = colour(tex, uv)
                if c is None:
                    continue
                world = [view @ m @ np.array([*p, 1.0]) for p in pts]
                depth = sum(w[2] for w in world) / 4
                shade = {"up": 1.0, "down": 0.55, "north": 0.8, "south": 0.8, "east": 0.68, "west": 0.68}[name]
                polys.append((depth, [(w[0], -w[1]) for w in world], tuple(min(255, int(ch * shade + 25)) for ch in c)))
    if not polys:
        return Image.new("RGB", (size, size), "white")
    xs = [x for _, pts, _ in polys for x, _ in pts]; ys = [y for _, pts, _ in polys for _, y in pts]
    span = max(max(xs) - min(xs), max(ys) - min(ys)) * 1.1
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    img = Image.new("RGB", (size, size), (200, 220, 245))
    d = ImageDraw.Draw(img)
    for _, pts, c in sorted(polys, key=lambda p: p[0]):
        d.polygon([((x - cx) / span * size + size / 2, (y - cy) / span * size + size / 2) for x, y in pts],
                  fill=c, outline=(30, 30, 30))
    return img


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "/tmp/dragon-preview.png"
    ticks = int(sys.argv[2]) if len(sys.argv) > 2 else 0
    with open(os.path.join(RES, "rigs", "ender_dragon.json")) as f:
        rig = json.load(f)
    tex = texture()
    views = [(0, 10, "front"), (90, 10, "side"), (180, 10, "back"), (45, 35, "three-quarter"), (0, 89, "top"),
             (135, -20, "below")]
    sheet = Image.new("RGB", (420 * 3, 420 * 2), "white")
    d = ImageDraw.Draw(sheet)
    for i, (yaw, pitch, label) in enumerate(views):
        img = render(rig, tex, yaw + 180, pitch, ticks)
        sheet.paste(img, ((i % 3) * 420, (i // 3) * 420))
        d.text(((i % 3) * 420 + 8, (i // 3) * 420 + 8), label, fill=(0, 0, 0))
    sheet.save(out)
    print(out)


if __name__ == "__main__":
    main()
