#!/bin/bash
# Sets the network up on a machine: Java 25, tmux and git, a build, every secret the launcher needs, and
# then the start. Safe to run again - every question shows what is set, and Enter keeps it.
#
#   ./install.sh

cd "$(dirname "$0")" || exit 1
ENV_FILE=.env.local
JAR=ServerLauncherApplication/target/ServerLauncher.jar
SESSION_NAME=server

yes_no() {
  local answer hint="[j/N]"
  [ "$2" = yes ] && hint="[J/n]"
  read -r -p "$1 $hint " answer
  case "${answer,,}" in
    j*|y*) return 0 ;;
    n*) return 1 ;;
    *) [ "$2" = yes ] ;;
  esac
}

# sets KEY=value in .env.local and leaves every other line alone
set_env() {
  touch "$ENV_FILE"
  grep -v "^$1=" "$ENV_FILE" > "$ENV_FILE.tmp"
  echo "$1=$2" >> "$ENV_FILE.tmp"
  mv "$ENV_FILE.tmp" "$ENV_FILE"
}

java_major() {
  "$1" -version 2>&1 | awk -F'"' '/version/ { split($2, v, "."); print v[1]; exit }'
}

# the first java 25 or newer: on the PATH, in JAVA_HOME, or one of the usual places JDKs are unpacked to
find_java() {
  local candidate major
  for candidate in "$(command -v java)" "${JAVA_HOME:+$JAVA_HOME/bin/java}" /usr/lib/jvm/*/bin/java \
      "$HOME"/.jdks/*/bin/java "$HOME"/.sdkman/candidates/java/*/bin/java; do
    [ -x "$candidate" ] || continue
    major=$(java_major "$candidate")
    if [ -n "$major" ] && [ "$major" -ge 25 ]; then
      readlink -f "$candidate"
      return 0
    fi
  done
  return 1
}

apt_install() {
  if ! command -v apt-get >/dev/null; then
    echo "Bitte $* von Hand installieren und das Skript danach noch einmal starten."
    exit 1
  fi
  yes_no "$* mit apt installieren?" yes || exit 1
  sudo apt-get update && sudo apt-get install -y "$@" || exit 1
}

echo "== Voraussetzungen =="
missing=()
for tool in git tmux; do
  command -v "$tool" >/dev/null || missing+=("$tool")
done
[ ${#missing[@]} -gt 0 ] && apt_install "${missing[@]}"

JAVA_BIN=$(find_java)
if [ -z "$JAVA_BIN" ]; then
  echo "Paper braucht Java 25, gefunden wurde keins."
  apt_install openjdk-25-jdk-headless
  JAVA_BIN=$(find_java) || { echo "Java 25 ist immer noch nicht zu finden."; exit 1; }
fi
JAVA_HOME=$(dirname "$(dirname "$JAVA_BIN")")
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
# run.sh reads this file before it builds or starts, so the launcher gets this java even if the PATH has another
set_env JAVA_HOME "$JAVA_HOME"
set_env PATH "\$JAVA_HOME/bin:\$PATH"
echo "Java $(java_major "$JAVA_BIN"): $JAVA_HOME"

echo
echo "== Bauen =="
if [ ! -f "$JAR" ] || yes_no "Es gibt schon einen Build. Neu bauen?" no; then
  ./mvnw -B clean install package || { echo "Der Build ist fehlgeschlagen."; exit 1; }
fi

"$JAVA_BIN" -cp "$JAR" de.hems.setup.Installer || exit 1

if tmux has-session -t "$SESSION_NAME" 2>/dev/null; then
  echo "Das Netzwerk läuft schon in der tmux-Session '$SESSION_NAME'. Die neuen Einstellungen gelten nach"
  echo "einem Neustart (/neustart 1 im Spiel). Ansehen mit: tmux attach -t $SESSION_NAME"
  exit 0
fi

if yes_no "Jetzt starten?" yes; then
  echo "Der Launcher läuft in tmux. Loslösen mit Strg+B, dann D - er läuft weiter."
  exec ./start.sh
fi
echo "Starten später mit ./start.sh"
