#!/bin/bash
# Starts the network in the tmux session "server" and attaches to it.
#
# The session outlives the launcher: after Ctrl-C, a crash or "/neustart ... aus" it is still there, with
# an empty shell in it. Attaching to it then started nothing - the network stayed off, or worse, the servers
# that were still running kept going without a launcher. So: the session is reused, but run.sh is started in
# it whenever no launcher is running.

cd "$(dirname "$0")" || exit 1
SESSION_NAME="server"

launcher_running() {
  pgrep -f "java -jar ServerLauncherApplication/target/ServerLauncher.jar" >/dev/null
}

if ! tmux has-session -t "$SESSION_NAME" 2>/dev/null; then
  echo "Erstelle tmux Session: $SESSION_NAME"
  tmux new-session -d -s "$SESSION_NAME" -c "$PWD"
  # run.sh baut, startet den Launcher und startet ihn nach /neustart wieder (mit oder ohne Update)
  tmux send-keys -t "$SESSION_NAME" "./run.sh" C-m
elif launcher_running; then
  echo "Der Launcher läuft schon in der tmux Session '$SESSION_NAME'."
else
  echo "Die tmux Session '$SESSION_NAME' gibt es noch, aber kein Launcher läuft darin - starte ihn."
  tmux send-keys -t "$SESSION_NAME" C-c
  tmux send-keys -t "$SESSION_NAME" "cd '$PWD' && ./run.sh" C-m
fi

tmux attach -t "$SESSION_NAME"
