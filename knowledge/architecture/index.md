# Component

* [Bedwars-Plugin (Bedwars)](bedwars.md) - Bedwars-Minispiel: eine Runde pro Server, mit Phasen, Shop, Upgrades, Endgame und schaltbaren Addons.
* [Gemeinsamer Code (CommonCode)](common-code.md) - Netzwerk-Events, ServerApi, gemeinsame Typen und Paper-Bausteine, die alle anderen Module benutzen.
* [Hunger-Games-Plugin (HungerGamesPlugin)](hunger-games.md) - Arena eines Hunger-Games-Events mit Füllhorn, Supply Drops, schrumpfender Grenze und Wertung.
* [Launcher (ServerLauncherApplication)](launcher.md) - Startet Proxy und Server, hält die netzwerkweiten Daten und betreibt Discord-Bot und Admin-Website.
* [Lobby-Plugin (LobbyPlugin)](lobby.md) - Lobby-Server mit Parkour, NPCs, Server Manager und dem Start eigener Runden.
* [Poker-Plugin (PokerPlugin)](poker.md) - Casino-Server einer Pokernacht mit Regeln, Tischen, Bots und Bank, gespielt um Bits.
* [Rucksack-Plugin (BackpackPlugin)](backpack.md) - Geteilter Team-Rucksack.
* [Speedrun-Plugin (RunPlugin)](run-plugin.md) - Macht aus einem leeren Paper-Server den Server für einen Speedrun-Versuch: Bosse, Hardcore, Zeit und /reset.
* [Survival-Plugin (Survival)](survival.md) - Survival-Spielmodus mit Teams, Shops, Marktplatz, Chunk Limiter und angepassten Paper-Einstellungen.
* [Velocity-Plugin (VelocityPlugin)](velocity.md) - Meldet neu angelegte Server am laufenden Proxy an, damit man sofort auf sie warpen kann.

# Interface

* [Admin-Website](admin-website.md) - Browser-Oberfläche des Launchers für Admins, mit Login per Passwort und Authenticator, Panels und Live-Konsole.
* [Netzwerk-Events](network-events.md) - Der JGroups-Cluster MCServer, über den Launcher, Proxy und Server einander Events schicken und Anfragen beantworten.
* [ServerApi](server-api.md) - Programmatischer Weg, Server im Netzwerk anzulegen, zu starten, zu stoppen und aufzulisten.
