#!/bin/bash
# Runs the launcher, and does what it asks for when it ends.
#
# The launcher ends with a code that says what comes next (see RestartMode):
#   10  restart on the code that is here
#   11  update: git pull, build, restart - back to the old code if either fails
#   anything else: stay off (0 is /neustart ... aus, everything else a crash)
#
# The first start updates, the way start.sh always did.

cd "$(dirname "$0")" || exit 1
RESULT=update-result.txt
JAR=ServerLauncherApplication/target/ServerLauncher.jar

note() {
  echo "$(date '+%F %T') $1" | tee "$RESULT"
}

build() {
  ./mvnw -B clean install package
}

update() {
  local before after
  before=$(git rev-parse --short HEAD)

  # a pull on top of local changes can conflict, and a rollback would throw them away - so neither happens
  if ! git diff --quiet || ! git diff --cached --quiet; then
    note "Update übersprungen: lokale Änderungen im Arbeitsverzeichnis. Läuft auf $before."
    [ -f "$JAR" ] || build
    return
  fi

  if ! git pull --ff-only; then
    note "git pull fehlgeschlagen. Läuft weiter auf $before."
    [ -f "$JAR" ] || build
    return
  fi

  after=$(git rev-parse --short HEAD)
  if [ "$before" = "$after" ] && [ -f "$JAR" ]; then
    note "Kein neuer Stand. Läuft auf $before."
    return
  fi

  if build; then
    note "Update $before -> $after erfolgreich."
    return
  fi

  # the new code does not build: back to what ran before, so the network comes back rather than staying off
  git reset --hard "$before"
  if build; then
    note "Build von $after fehlgeschlagen - zurück auf $before."
  else
    note "Build von $after fehlgeschlagen, und $before baut auch nicht mehr. Bitte von Hand ansehen."
  fi
}

MODE=update
while true; do
  if [ "$MODE" = update ]; then
    update
  fi
  # settings of this machine, e.g. how much memory a server may get. Not in git: it describes the box
  [ -f .env.local ] && set -a && . ./.env.local && set +a
  java -version
  java -jar "$JAR"
  CODE=$?
  case $CODE in
    10) MODE=restart; echo "Neustart." ;;
    11) MODE=update; echo "Neustart mit Update." ;;
    *) echo "Der Launcher hat mit $CODE beendet - das Netzwerk bleibt aus."; exit "$CODE" ;;
  esac
done
