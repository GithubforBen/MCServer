#!/bin/bash
# Takes the lobby as it is now into the map that ships with the network (assets/lobby-spawn.zip):
# the world, and what the lobby plugin keeps about it - parkours, NPCs, the lotto stand.
#
# Workflow for changing the lobby:
#   1. in servers/LOBBY/plugins/LobbyPlugin/config.yml set  lobby.restore-on-start: false
#      (otherwise every start puts the old map back and what was built is gone)
#   2. build in the lobby, set up parkours (/parkour setup), NPCs (/npc), the lotto stand (/lotto stand)
#   3. stop the lobby (or the network) so the world is saved, then run this script
#   4. set restore-on-start back to true, raise the LOBBY_SPAWN version in FileType.ASSET.getVersion
#      so other installations take the new map, commit assets/lobby-spawn.zip
#
# A fresh network unpacks the zip on its first start, and the lobby is restored from it on every start.

set -e
cd "$(dirname "$0")"

SERVER=servers/LOBBY
WORLD=$SERVER/world
DATA=$SERVER/plugins/LobbyPlugin
OUT=assets/lobby-spawn.zip

[ -f "$WORLD/level.dat" ] || { echo "No lobby world at $WORLD"; exit 1; }
if [ -f "$WORLD/session.lock" ] && command -v fuser >/dev/null && fuser "$WORLD/session.lock" >/dev/null 2>&1; then
  echo "The lobby is still running - stop it first, so the world on disk is complete."
  exit 1
fi

TMP=$(mktemp -d ./.lobby-snapshot.XXXXXX)
trap 'rm -rf "$TMP"' EXIT

# the map: the world without who was on it and without the identity of this installation
cp -r "$WORLD" "$TMP/lobby-world"
rm -rf "$TMP/lobby-world/players" "$TMP/lobby-world/playerdata" "$TMP/lobby-world/stats" \
       "$TMP/lobby-world/advancements" "$TMP/lobby-world/session.lock" "$TMP/lobby-world/uid.dat" "$TMP/lobby-world/level.dat_old"

# what the plugin keeps about the map; a file that is not there is shipped as nothing
mkdir -p "$TMP/plugins/LobbyPlugin"
for file in parkour.yml npcs.yml lotto-stand.yml; do
  if [ -f "$DATA/$file" ]; then cp "$DATA/$file" "$TMP/plugins/LobbyPlugin/"; else : > "$TMP/plugins/LobbyPlugin/$file"; fi
done

# the spawn of the map is the spawn of the world, which /setworldspawn sets
# keep the server's world in sync with the map it now ships
rm -rf "$SERVER/lobby-world"
cp -r "$TMP/lobby-world" "$SERVER/lobby-world"

rm -f "$OUT"
(cd "$TMP" && zip -q -r -X "../$OUT" lobby-world plugins)
echo "Wrote $OUT ($(du -h "$OUT" | cut -f1)): world + $(ls "$TMP/plugins/LobbyPlugin" | tr '\n' ' ')"
