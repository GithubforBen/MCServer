#!/bin/bash
# make-xray-pack.sh [version] - builds the resource pack an x-ray cheater would use, to test anti-xray with:
# stone, deepslate, dirt and the other ground blocks get no model (invisible), and every ore is drawn from all
# sides (no face culling), so an ore shows through the ground - unless the server sent it as stone.
# Installs it into ~/.minecraft/resourcepacks/xraytest; enable it in Options -> Resource Packs.
VERSION=${1:-26.3}
JAR=~/.minecraft/versions/$VERSION/$VERSION.jar
[ -f "$JAR" ] || { echo "No client jar at $JAR"; exit 1; }
FORMAT=$(unzip -p "$JAR" version.json | python3 -c "import json,sys;print(json.load(sys.stdin)['pack_version']['resource_major'])")
P=~/.minecraft/resourcepacks/xraytest
rm -rf "$P"; mkdir -p "$P/assets/minecraft/models/block"
echo "{\"pack\": {\"description\": \"X-Ray-Test\", \"pack_format\": $FORMAT, \"min_format\": [$FORMAT, 0], \"max_format\": [$FORMAT, 9]}}" > "$P/pack.mcmeta"
for m in stone stone_mirrored deepslate deepslate_mirrored tuff granite diorite andesite dirt gravel netherrack \
         calcite smooth_basalt basalt blackstone; do
  echo '{"textures": {"particle": "minecraft:block/stone"}, "elements": []}' > "$P/assets/minecraft/models/block/$m.json"
done
python3 - "$P/assets/minecraft/models/block" <<'PY'
import json, os, sys
ores = ["diamond_ore", "deepslate_diamond_ore", "iron_ore", "deepslate_iron_ore", "gold_ore", "deepslate_gold_ore",
        "coal_ore", "deepslate_coal_ore", "emerald_ore", "deepslate_emerald_ore", "lapis_ore", "deepslate_lapis_ore",
        "redstone_ore", "deepslate_redstone_ore", "copper_ore", "deepslate_copper_ore", "nether_gold_ore",
        "nether_quartz_ore", "ancient_debris"]
for ore in ores:
    texture = "minecraft:block/" + (ore + "_side" if ore == "ancient_debris" else ore)
    faces = {d: {"uv": [0, 0, 16, 16], "texture": "#all"} for d in ["down", "up", "north", "south", "east", "west"]}
    model = {"textures": {"all": texture, "particle": texture},
             "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}
    json.dump(model, open(os.path.join(sys.argv[1], ore + ".json"), "w"))
PY
echo "Pack (format $FORMAT) written to $P"
