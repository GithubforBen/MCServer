#!/bin/bash
# setup.sh - unpacks xdotool (and its library) into $MCTEST_DIR without sudo. Run once per machine.
X=${MCTEST_DIR:-$HOME/.cache/mctest}
mkdir -p "$X/deb" "$X/x" && cd "$X/deb" || exit 1
apt-get download xdotool libxdo3 || { echo "apt-get download failed"; exit 1; }
for deb in *.deb; do dpkg -x "$deb" "$X/x"; done
"$(dirname "$0")/xdo" version && echo "xdotool is ready in $X/x"
