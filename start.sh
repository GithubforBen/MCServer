#!/bin/bash

SESSION_NAME="server"

# Prüfen, ob die Session bereits existiert
tmux has-session -t "$SESSION_NAME" 2>/dev/null

if [ $? != 0 ]; then
  echo "Erstelle tmux Session: $SESSION_NAME"
  tmux new-session -d -s "$SESSION_NAME"

  # run.sh baut, startet den Launcher und startet ihn nach /neustart wieder (mit oder ohne Update)
  tmux send-keys -t "$SESSION_NAME" "./run.sh" C-m
else
  echo "tmux Session '$SESSION_NAME' existiert bereits."
fi

# Optional: automatisch an die Session anhängen
tmux attach -t "$SESSION_NAME"
