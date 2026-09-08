#!/usr/bin/env python3
"""Convert third-party Minecraft builds into the mod's landmark format.

Four sources arrive in four different formats:

  * Sponge schematic v3 (.schem)   - modern, palette based
  * MCEdit schematic (.schematic)  - pre-1.13, numeric block ids + nibble data
  * superflat world saves (Anvil)  - the build sits above a known flat layer

All of them are normalised to one gzipped NBT file per landmark, which the
mod reads with vanilla NbtIo. The schema is deliberately dumb:

  Id, Name      strings
  Width/Height/Length  ints
  Palette       list of block state strings ("minecraft:stone_slab[type=top]")
  Positions     int array, packed as (y * Length + z) * Width + x
  States        int array, parallel to Positions, index into Palette

Only non-air cells are stored, so a mostly empty bounding box costs nothing.
"""

import gzip, io, os, struct, sys, zlib
from collections import OrderedDict

# --------------------------------------------------------------- NBT read

def _rd(f, fmt):
    return struct.unpack(fmt, f.read(struct.calcsize(fmt)))[0]

def _rstr(f):
    return f.read(_rd(f, ">H")).decode("utf-8", "replace")

def _rpay(f, t):
    if t == 1:  return _rd(f, ">b")
    if t == 2:  return _rd(f, ">h")
    if t == 3:  return _rd(f, ">i")
    if t == 4:  return _rd(f, ">q")
    if t == 5:  return _rd(f, ">f")
    if t == 6:  return _rd(f, ">d")
    if t == 7:  return f.read(_rd(f, ">i"))
    if t == 8:  return _rstr(f)
    if t == 9:
        it = _rd(f, ">b"); n = _rd(f, ">i")
        if n <= 0 or it == 0:
            return []
        return [_rpay(f, it) for _ in range(n)]
    if t == 10:
        d = OrderedDict()
        while True:
            tt = _rd(f, ">b")
            if tt == 0:
                return d
            # Name before payload: `d[k()] = v()` evaluates the value first.
            name = _rstr(f)
            d[name] = _rpay(f, tt)
    if t == 11:
        n = _rd(f, ">i"); return list(struct.unpack(">%di" % n, f.read(4 * n)))
    if t == 12:
        n = _rd(f, ">i"); return list(struct.unpack(">%dq" % n, f.read(8 * n)))
    raise ValueError("unknown NBT tag %d" % t)

def nbt_load(data):
    if data[:2] == b"\x1f\x8b":
        data = gzip.decompress(data)
    elif data[:1] == b"\x78":
        data = zlib.decompress(data)
    f = io.BytesIO(data)
    t = _rd(f, ">b")
    if t == 0:
        return OrderedDict()
    _rstr(f)
    return _rpay(f, t)

# -------------------------------------------------------------- NBT write

def _wstr(out, s):
    b = s.encode("utf-8")
    out.write(struct.pack(">H", len(b))); out.write(b)

def _wtag(out, t, name, value):
    out.write(struct.pack(">b", t)); _wstr(out, name); _wpay(out, t, value)

def _wpay(out, t, v):
    if t == 3:  out.write(struct.pack(">i", v)); return
    if t == 8:  _wstr(out, v); return
    if t == 9:
        out.write(struct.pack(">b", 8)); out.write(struct.pack(">i", len(v)))
        for s in v: _wstr(out, s)
        return
    if t == 11:
        out.write(struct.pack(">i", len(v)))
        out.write(struct.pack(">%di" % len(v), *v)); return
    raise ValueError(t)

def nbt_write(path, fields):
    body = io.BytesIO()
    body.write(struct.pack(">b", 10)); _wstr(body, "")
    for t, name, value in fields:
        _wtag(body, t, name, value)
    body.write(struct.pack(">b", 0))
    with gzip.open(path, "wb") as fh:
        fh.write(body.getvalue())

# ------------------------------------------------------------ state names

def state_name(name, props):
    if not props:
        return name
    inner = ",".join("%s=%s" % (k, props[k]) for k in sorted(props))
    return "%s[%s]" % (name, inner)

# ------------------------------------------------- Sponge schematic v2/v3

def read_schem(data):
    root = nbt_load(data)
    root = root.get("Schematic", root)
    w, h, l = root["Width"], root["Height"], root["Length"]
    container = root.get("Blocks", root)
    palette_raw = container["Palette"]
    blockdata = container.get("Data", container.get("BlockData"))

    # Palette maps state string -> index; invert it.
    order = [None] * len(palette_raw)
    for state, idx in palette_raw.items():
        order[idx] = state

    out = []
    i = 0
    pos = 0
    n = len(blockdata)
    while i < n:
        val = 0; shift = 0
        while True:
            b = blockdata[i]; i += 1
            val |= (b & 0x7F) << shift
            if not (b & 0x80):
                break
            shift += 7
        state = order[val]
        # Sponge palettes include air; storing it would balloon the file to
        # the full bounding box (1.6M cells for the Eiffel Tower) and make
        # placement spend most of its budget writing nothing.
        if state not in ("minecraft:air", "minecraft:cave_air", "minecraft:void_air"):
            y, rem = divmod(pos, w * l)
            z, x = divmod(rem, w)
            out.append((x, y, z, state))
        pos += 1
    return w, h, l, out

# ------------------------------------------------- MCEdit legacy .schematic

# Only the ids these files actually use. Data values that change the visible
# block (wool colour, slab material, stair facing) are mapped; the rest fall
# back to the plain form.
_WOOL = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
         "gray", "light_gray", "cyan", "purple", "blue", "brown", "green",
         "red", "black"]
_SLAB = ["smooth_stone", "sandstone", "oak", "cobblestone", "brick",
         "stone_brick", "nether_brick", "quartz"]
_STAIR_FACING = ["east", "west", "south", "north"]

def _legacy(bid, dv):
    if bid == 0:   return None
    if bid == 35:  return "minecraft:%s_wool" % _WOOL[dv & 15]
    if bid == 95:  return "minecraft:%s_stained_glass" % _WOOL[dv & 15]
    if bid == 49:  return "minecraft:obsidian"
    if bid == 82:  return "minecraft:clay"
    if bid == 113: return "minecraft:nether_brick_fence"
    if bid == 98:
        return ["minecraft:stone_bricks", "minecraft:mossy_stone_bricks",
                "minecraft:cracked_stone_bricks", "minecraft:chiseled_stone_bricks"][dv & 3]
    if bid == 155:
        return ["minecraft:quartz_block", "minecraft:chiseled_quartz_block",
                "minecraft:quartz_pillar"][min(dv & 3, 2)]
    if bid == 43:
        mat = _SLAB[dv & 7]
        return "minecraft:smooth_stone" if mat == "smooth_stone" else "minecraft:%s_block" % mat \
            if mat == "quartz" else "minecraft:%s_slab[type=double]" % mat
    if bid == 44:
        mat = _SLAB[dv & 7]
        half = "top" if dv & 8 else "bottom"
        return "minecraft:%s_slab[type=%s]" % (mat, half)
    if bid in (109, 156):
        base = "stone_brick" if bid == 109 else "quartz"
        facing = _STAIR_FACING[dv & 3]
        half = "top" if dv & 4 else "bottom"
        return "minecraft:%s_stairs[facing=%s,half=%s,shape=straight,waterlogged=false]" % (
            base, facing, half)
    return "minecraft:stone"

def read_legacy(data):
    root = nbt_load(data)
    root = root.get("Schematic", root)
    w, h, l = root["Width"], root["Height"], root["Length"]
    blocks = root["Blocks"]
    extra = root.get("Data") or b"\x00" * len(blocks)
    out = []
    for i, bid in enumerate(blocks):
        state = _legacy(bid, extra[i] if i < len(extra) else 0)
        if state is None:
            continue
        y, rem = divmod(i, w * l)
        z, x = divmod(rem, w)
        out.append((x, y, z, state))
    return w, h, l, out

# ------------------------------------------------------ Anvil world saves

def _chunks(region_path):
    with open(region_path, "rb") as fh:
        header = fh.read(4096)
        if len(header) < 4096:
            return
        for n in range(1024):
            off = int.from_bytes(header[n * 4:n * 4 + 3], "big")
            count = header[n * 4 + 3]
            if off == 0 or count == 0:
                continue
            fh.seek(off * 4096)
            head = fh.read(5)
            if len(head) < 5:
                continue
            length = int.from_bytes(head[:4], "big")
            comp = head[4]
            payload = fh.read(length - 1)
            try:
                if comp == 1:   payload = gzip.decompress(payload)
                elif comp == 2: payload = zlib.decompress(payload)
                yield nbt_load(payload)
            except Exception:
                continue

def read_flat_world(world_dir, ground_top):
    """Everything strictly above `ground_top` is the build."""
    out = []
    region_dir = os.path.join(world_dir, "region")
    for fn in sorted(os.listdir(region_dir)):
        if not fn.endswith(".mca"):
            continue
        for chunk in _chunks(os.path.join(region_dir, fn)):
            cx, cz = chunk.get("xPos"), chunk.get("zPos")
            if cx is None:
                continue
            for sec in chunk.get("sections", []):
                bs = sec.get("block_states")
                if not bs:
                    continue
                palette = bs.get("palette", [])
                if not palette:
                    continue
                names = [state_name(p["Name"], p.get("Properties")) for p in palette]
                if len(names) == 1:
                    if names[0] == "minecraft:air":
                        continue
                    idx_at = lambda i: 0
                else:
                    data = bs.get("data")
                    if not data:
                        continue
                    bits = max(4, (len(names) - 1).bit_length())
                    per = 64 // bits
                    mask = (1 << bits) - 1
                    def idx_at(i, data=data, bits=bits, per=per, mask=mask):
                        word = data[i // per]
                        return (word >> ((i % per) * bits)) & mask
                base_y = sec["Y"] * 16
                if base_y + 15 <= ground_top:
                    continue
                for i in range(4096):
                    y = base_y + (i >> 8)
                    if y <= ground_top:
                        continue
                    state = names[idx_at(i)]
                    if state == "minecraft:air":
                        continue
                    z = (i >> 4) & 15
                    x = i & 15
                    out.append((cx * 16 + x, y, cz * 16 + z, state))
    return out

# ---------------------------------------------------------------- writing

def normalise_and_write(landmark_id, display, cells, out_path):
    if not cells:
        raise SystemExit("no blocks found for %s" % landmark_id)
    xs = [c[0] for c in cells]; ys = [c[1] for c in cells]; zs = [c[2] for c in cells]
    minx, miny, minz = min(xs), min(ys), min(zs)
    w = max(xs) - minx + 1
    h = max(ys) - miny + 1
    l = max(zs) - minz + 1

    palette = OrderedDict()
    positions = []
    states = []
    for x, y, z, state in cells:
        if state not in palette:
            palette[state] = len(palette)
        px, py, pz = x - minx, y - miny, z - minz
        positions.append((py * l + pz) * w + px)
        states.append(palette[state])

    nbt_write(out_path, [
        (8,  "Id", landmark_id),
        (8,  "Name", display),
        (3,  "Width", w), (3, "Height", h), (3, "Length", l),
        (9,  "Palette", list(palette.keys())),
        (11, "Positions", positions),
        (11, "States", states),
    ])
    print("  %-22s %3d x %3d x %3d  %8d blocks  %3d states  %6.1f KB"
          % (landmark_id, w, h, l, len(positions), len(palette),
             os.path.getsize(out_path) / 1024.0))

# ------------------------------------------------------------------- main

def main():
    dl = os.path.expanduser("~/Downloads")
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    print("Converting landmarks ->", out_dir)

    w, h, l, cells = read_schem(open(os.path.join(dl, "copper-eiffel-tower/Copper_Eiffel_Tower.schem"), "rb").read())
    normalise_and_write("eiffel_tower", "Eiffel Tower", cells, os.path.join(out_dir, "eiffel_tower.nbt"))

    w, h, l, cells = read_legacy(open(os.path.join(dl, "concorde/ConcordeBritishNew(landed).schematic"), "rb").read())
    normalise_and_write("concorde", "Concorde", cells, os.path.join(out_dir, "concorde.nbt"))

    cells = read_flat_world(os.path.join(dl, "Burj Khalifa 1_1"), ground_top=-61)
    normalise_and_write("burj_khalifa", "Burj Khalifa", cells, os.path.join(out_dir, "burj_khalifa.nbt"))

    cells = read_flat_world(os.path.join(dl, "leaning tower of pisa"), ground_top=-43)
    normalise_and_write("leaning_tower_of_pisa", "Leaning Tower of Pisa", cells, os.path.join(out_dir, "leaning_tower_of_pisa.nbt"))

if __name__ == "__main__":
    main()
