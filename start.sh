#!/bin/bash

SESSION_NAME="server"

# Prüfen, ob die Session bereits existiert
tmux has-session -t "$SESSION_NAME" 2>/dev/null

if [ $? != 0 ]; then
  echo "Erstelle tmux Session: $SESSION_NAME"
  tmux new-session -d -s "$SESSION_NAME"

  # Befehle in der tmux Session ausführen
  tmux send-keys -t "$SESSION_NAME" "git pull" C-m
  # Einstellungen dieser Maschine, z.B. wie viel RAM ein Server bekommen darf. Die Datei liegt
  # bewusst nicht im git: sie beschreibt die Kiste, nicht das Netzwerk.
  tmux send-keys -t "$SESSION_NAME" "[ -f .env.local ] && set -a && . ./.env.local && set +a" C-m
  tmux send-keys -t "$SESSION_NAME" "java -version" C-m
  tmux send-keys -t "$SESSION_NAME" "./mvnw clean install package" C-m
  tmux send-keys -t "$SESSION_NAME" "java -jar ServerLauncherApplication/target/ServerLauncher.jar" C-m
else
  echo "tmux Session '$SESSION_NAME' existiert bereits."
fi

# Optional: automatisch an die Session anhängen
tmux attach -t "$SESSION_NAME"
