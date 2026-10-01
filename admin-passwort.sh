#!/bin/bash
# Sets a new password for an account of the admin website (and on request a new 2FA key).
# The old password can not be shown: only its hash is stored. Works while the network runs - the next
# login uses the new password.
#
#   ./admin-passwort.sh
cd "$(dirname "$0")" || exit 1
[ -f .env.local ] && set -a && . ./.env.local && set +a
JAR=ServerLauncherApplication/target/ServerLauncher.jar
[ -f "$JAR" ] || { echo "Kein Build gefunden ($JAR) - erst ./install.sh oder ./mvnw install."; exit 1; }
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$JAR" de.hems.setup.Installer passwort
