"""
The mini ender dragon: vanilla's EnderDragonModel (createBodyLayer, Minecraft 26.3) as item models, one per moving part,
and the rig that puts them together — for Core's companion rigs, because the client draws the real ender dragon at full
size whatever its scale attribute says.

    python3 rig.py     # writes ../../src/main/resources/de/raindancer/modules/pets/pack/... and .../rigs/ender_dragon.json

The texture is the client's own (minecraft:entity/enderdragon/dragon), pulled into the blocks atlas by an atlas source —
nothing of Mojang's is copied. Units: entity model pixels, y pointing down, as in the game's model code; the rig turns it
the right way up (scale -1, -1, 1) like the game's renderer.
"""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "src", "main", "resources", "de", "raindancer", "modules", "pets")
PACK = os.path.join(OUT, "pack")
NS = "rainspets"
TEX = "minecraft:entity/enderdragon/dragon"
TEXTURE_SIZE = 256


def box(x, y, z, w, h, d, u, v, mirror=False, rot=None):
    """rot: (axis, degrees, origin) — an element rotation inside the part, already snapped."""
    return dict(x=x, y=y, z=z, w=w, h=h, d=d, u=u, v=v, mirror=mirror, rot=rot)


SEGMENT = [box(-5, -5, -5, 10, 10, 10, 192, 104), box(-1, -9, -3, 2, 4, 6, 48, 0)]

# name, parent, pivot (from the parent's pivot), rotation (x, y, z radians), boxes, motion — EnderDragonModel's
# createBodyLayer, posed by its setupAnim (in Core's RigPose): everything hangs on a root joint at z -48 that bobs with
# the beat; neck and tail are chains of segments that set each other's place.
PARTS = [
    ("root", None, (0, 0, -48), (0, 0, 0), [], {"kind": "root"}),
    ("body", "root", (0, 20, 8), (0, 0, 0),
     [box(-12, -16, -16, 24, 24, 64, 0, 0)] + [box(-1, -22, z, 2, 6, 12, 220, 53) for z in (-10, 10, 30)], None),
    *[(f"neck_{i}", "root", (0, 20, -12), (0, 0, 0), SEGMENT, {"kind": "neck", "index": i}) for i in range(5)],
    ("head", "root", (0, 0, 0), (0, 0, 0),
     [box(-6, -1, -24, 12, 5, 16, 176, 44), box(-8, -8, -10, 16, 16, 16, 112, 30),
      box(-5, -12, -4, 2, 4, 6, 0, 0, True), box(-5, -3, -22, 2, 2, 4, 112, 0, True),
      box(3, -12, -4, 2, 4, 6, 0, 0), box(3, -3, -22, 2, 2, 4, 112, 0)], {"kind": "head"}),
    ("jaw", "head", (0, 4, -8), (0, 0, 0), [box(-6, 0, -16, 12, 4, 16, 176, 65)], {"kind": "jaw"}),
    *[(f"tail_{i}", "root", (0, 10, 60), (0, 0, 0), SEGMENT, {"kind": "tail", "index": i}) for i in range(12)],
    ("left_wing", "body", (12, -15, -6), (0, 0, 0),
     [box(0, -4, -4, 56, 8, 8, 112, 88, True), box(0, 0, 2, 56, 0, 56, -56, 88, True)], {"kind": "wing", "side": 1}),
    ("left_wing_tip", "left_wing", (56, 0, 0), (0, 0, 0),
     [box(0, -2, -2, 56, 4, 4, 112, 136, True), box(0, 0, 2, 56, 0, 56, -56, 144, True)],
     {"kind": "wing_tip", "side": 1}),
    ("right_wing", "body", (-12, -15, -6), (0, 0, 0),
     [box(-56, -4, -4, 56, 8, 8, 112, 88), box(-56, 0, 2, 56, 0, 56, -56, 88)], {"kind": "wing", "side": -1}),
    ("right_wing_tip", "right_wing", (-56, 0, 0), (0, 0, 0),
     [box(-56, -2, -2, 56, 4, 4, 112, 136), box(-56, 0, 2, 56, 0, 56, -56, 144)], {"kind": "wing_tip", "side": -1}),
    *[part for side, x in (("left", 1), ("right", -1)) for part in (
        (f"{side}_front_leg", "body", (12 * x, 0, -6), (1.3, 0, 0), [box(-4, -4, -4, 8, 24, 8, 112, 104)],
         {"kind": "leg", "side": 1}),
        (f"{side}_front_leg_tip", f"{side}_front_leg", (0, 20, -1), (-0.5, 0, 0),
         [box(-3, -1, -3, 6, 24, 6, 226, 138)], {"kind": "leg", "side": -1}),
        (f"{side}_front_foot", f"{side}_front_leg_tip", (0, 23, 0), (0.75, 0, 0),
         [box(-4, 0, -12, 8, 4, 16, 144, 104)], {"kind": "leg", "side": 1}),
        (f"{side}_hind_leg", "body", (16 * x, -4, 34), (1.0, 0, 0), [box(-8, -4, -8, 16, 32, 16, 0, 0)],
         {"kind": "leg", "side": 1}),
        (f"{side}_hind_leg_tip", f"{side}_hind_leg", (0, 32, -4), (0.5, 0, 0),
         [box(-6, -2, 0, 12, 32, 12, 196, 0)], {"kind": "leg", "side": 1}),
        (f"{side}_hind_foot", f"{side}_hind_leg_tip", (0, 31, 4), (0.75, 0, 0),
         [box(-9, 0, -20, 18, 6, 24, 112, 0)], {"kind": "leg", "side": 1}),
    )],
]
# EnderDragonRenderer: translate(0, 0, 1), scale(-1, -1, 1), translate(0, -1.501, 0) — in blocks; here y and z instead
# put the body's middle (root -32 + body 20 - 4 = -16 px high, -24 px along) on the carrier, so the rig sits on it
OFFSET = [0, 1.0, 1.5]


def faces(b):
    u, v, w, h, d = b["u"], b["v"], b["w"], b["h"], b["d"]
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    regions = {
        "down": (f5, f10, f6, f11),   # the game's top: y is down in here, the rig turns it over
        "up": (f6, f10, f7, f11),
        "west": (f4, f11, f5, f12),
        "north": (f5, f11, f6, f12),
        "east": (f6, f11, f8, f12),
        "south": (f8, f11, f9, f12),
    }
    if h == 0:
        # a membrane: our own wing art, cut out cleanly at every distance (the game's sheet blurs into a haze when an
        # item model's mipmaps soften its see-through part), on both faces — the game draws it from both sides
        art = "#wing_tip" if b["v"] >= 144 else "#wing"
        full = [0, 0, 16, 16] if not b["mirror"] else [16, 0, 0, 16]
        return {"down": {"uv": full, "texture": art}, "up": {"uv": full, "texture": art}}
    if b["mirror"]:
        regions["west"], regions["east"] = regions["east"], regions["west"]
    out = {}
    k = 16 / TEXTURE_SIZE
    for name, (a, c, e, g) in regions.items():
        if e == a or g == c:
            continue
        uv = [a * k, c * k, e * k, g * k]
        if b["mirror"]:
            uv = [uv[2], uv[1], uv[0], uv[3]]
        out[name] = {"uv": [round(x, 4) for x in uv], "texture": "#dragon"}
    return out


def extent(boxes):
    lo, hi = [1e9] * 3, [-1e9] * 3
    for b in boxes:
        for i, (p, s) in enumerate(((b["x"], b["w"]), (b["y"], b["h"]), (b["z"], b["d"]))):
            lo[i] = min(lo[i], p)
            hi[i] = max(hi[i], p + s)
    return lo, hi


def model(boxes):
    lo, hi = extent(boxes)
    # model coordinates = 8 + k * pixels, kept inside the -16..32 an item model allows
    room = min(min((8 + 16) / max(1e-6, -l), (32 - 8) / max(1e-6, h)) for l, h in zip(lo, hi))
    k = min(1.0, 0.95 * room)
    elements = []
    for b in boxes:
        frm = [8 + k * b["x"], 8 + k * b["y"], 8 + k * b["z"]]
        to = [8 + k * (b["x"] + b["w"]), 8 + k * (b["y"] + max(b["h"], 0.01)), 8 + k * (b["z"] + b["d"])]
        e = {"from": [round(x, 4) for x in frm], "to": [round(x, 4) for x in to], "faces": faces(b)}
        if b["rot"]:
            axis, angle, origin = b["rot"]
            if angle:
                e["rotation"] = {"axis": axis, "angle": angle,
                                 "origin": [round(8 + k * o, 4) for o in origin]}
        elements.append(e)
    return k, {"textures": {"dragon": TEX, "particle": TEX, "wing": f"{NS}:block/mini_dragon_wing",
                            "wing_tip": f"{NS}:block/mini_dragon_wing_tip"}, "elements": elements,
               "display": {"fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]}}}


def wing_art(path, tip):
    """A membrane of our own: dark skin with a purple sheen, a finger bone across it, a scalloped trailing edge."""
    from PIL import Image, ImageDraw
    n = 64
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # u runs along the bone (0 at the shoulder), v away from it
    if tip:
        poly = [(0, 0), (n, 0), (n, 6)] + [(n - i, 6 + i * 0.85) for i in range(0, n, 4)] + [(0, n - 4)]
    else:
        poly = [(0, 0), (n, 0)] + [(n - i, 8 + (i ** 1.25) * 0.55) for i in range(0, n, 4)] + [(0, n)]
    d.polygon(poly, fill=(22, 20, 27, 255))
    # scallops eaten out of the trailing edge
    for i in range(6):
        cx = n * (0.2 + i * 0.15)
        cy = (n - 2) if not tip else (n * 0.9 - i * 8)
        d.ellipse((cx - 6, cy - 4, cx + 6, cy + 8), fill=(0, 0, 0, 0))
    # sheen and a finger bone
    for y in range(n):
        for x in range(n):
            r, g, b, a = img.getpixel((x, y))
            if a:
                t = (x + y) / (2 * n)
                img.putpixel((x, y), (int(r + 14 * t), int(g + 8 * t), int(b + 22 * t), 255))
    mask = img.getchannel("A")
    d.line((2, 1, n - 6, n - 10) if not tip else (2, 1, n - 4, n * 0.55), fill=(96, 92, 104, 255), width=2)
    d.line((0, 0, n, 0), fill=(70, 66, 76, 255), width=2)
    img.putalpha(mask)
    img.save(path)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=1)


def main():
    files = []
    rig = {"height": 100, "offset": OFFSET, "parts": []}
    for name, parent, pivot, rotation, boxes, motion in PARTS:
        if not boxes:
            rig["parts"].append({"name": name, "model": None, "parent": parent, "pivot": list(pivot),
                                 "rotation": list(rotation), "modelScale": 1, "motion": motion})
            continue
        k, data = model(boxes)
        key = "ender_dragon_segment" if name.startswith(("neck_", "tail_")) else f"ender_dragon_{name}"
        if f"assets/{NS}/items/{key}.json" not in files:
            write(os.path.join(PACK, "assets", NS, "models", "item", key + ".json"), data)
            write(os.path.join(PACK, "assets", NS, "items", key + ".json"),
                  {"model": {"type": "minecraft:model", "model": f"{NS}:item/{key}"}})
            files += [f"assets/{NS}/models/item/{key}.json", f"assets/{NS}/items/{key}.json"]
        rig["parts"].append({"name": name, "model": f"{NS}:{key}", "parent": parent, "pivot": list(pivot),
                             "rotation": list(rotation), "modelScale": round(k, 6), "motion": motion})
    for name, tip in (("mini_dragon_wing", False), ("mini_dragon_wing_tip", True)):
        png = os.path.join(PACK, "assets", NS, "textures", "block", name + ".png")
        os.makedirs(os.path.dirname(png), exist_ok=True)
        wing_art(png, tip)
        write(png + ".mcmeta", {"texture": {"mipmap_strategy": "strict_cutout"}})
        files += [f"assets/{NS}/textures/block/{name}.png", f"assets/{NS}/textures/block/{name}.png.mcmeta"]
    write(os.path.join(PACK, "assets", "minecraft", "atlases", "blocks.json"),
          {"sources": [{"type": "minecraft:single", "resource": TEX}]})
    files.append("assets/minecraft/atlases/blocks.json")
    write(os.path.join(PACK, "pack.mcmeta"),
          {"pack": {"description": "Rain's Pets — the mini ender dragon", "min_format": 88, "max_format": 97}})
    files.insert(0, "pack.mcmeta")
    write(os.path.join(OUT, "rigs", "ender_dragon.json"), rig)
    with open(os.path.join(PACK, "files.txt"), "w") as f:
        f.write("\n".join(files) + "\n")
    print(len(PARTS), "parts,", len(files), "pack files")


if __name__ == "__main__":
    main()
