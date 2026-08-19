package de.schnorrenbergers.survival.featrues.team;

import de.hems.api.UUIDFetcher;
import de.hems.paper.team.TeamService;
import de.hems.types.team.TeamData;
import de.hems.types.team.TeamSettings;
import de.schnorrenbergers.survival.Survival;
import de.schnorrenbergers.survival.featrues.money.MoneyHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Everything that can be done with a team, from the point of view of this server.
 * <p>
 * The team itself lives on the launcher; this is the piece that turns a player's wish into a write there
 * and reports back what came of it. Nothing is ever stored locally, so two servers can be used at once
 * without either of them holding a version of a team the other does not know about.
 * <p>
 * Every write is optimistic: the team carries the revision it was read at and the launcher refuses it if
 * somebody changed the team in the meantime. That turns a lost update into a message telling the player to
 * look again, which is the honest outcome.
 */
public class TeamManager {

    /** Where a pending invite is remembered on the invited player. */
    private static final String INVITE_KEY = "pending-team-invite";

    private final String name;
    private TeamData data;

    private TeamManager(String name, TeamData data) {
        this.name = name;
        this.data = data;
    }

    /**
     * @param name the team to work with
     * @return a manager for it, whether or not the team exists
     */
    public static TeamManager of(String name) {
        return new TeamManager(name, TeamService.getTeam(name));
    }

    /**
     * @param player the player whose team to work with
     * @return a manager for it, or {@code null} if the player has no team
     */
    public static TeamManager of(Player player) {
        TeamData team = TeamService.getTeamOf(player.getUniqueId());
        return team == null ? null : new TeamManager(team.getName(), team);
    }

    public boolean exists() {
        return data != null;
    }

    public TeamData getData() {
        return data;
    }

    public String getName() {
        return data == null ? name : data.getName();
    }

    public String getTag() {
        return data == null ? null : data.getTag();
    }

    public UUID getLeaderUUID() {
        return data == null ? null : data.getLeader();
    }

    public int getPlayerAmount() {
        return data == null ? 0 : data.getMembers().size();
    }

    /**
     * @return how many members this team may have, never above what the server allows
     */
    public int getMaxPlayerAmount() {
        int wanted = data == null ? 0 : data.getSettings().getNumber(TeamSettings.Key.MAX_MEMBERS);
        return Math.min(wanted, rules().getMaxMembersCap());
    }

    private static TeamRules rules() {
        return Survival.getInstance().getTeamRules();
    }

    /* ------------------------------------------------------------------ lifecycle */

    /**
     * Creates a team and makes the player its leader.
     *
     * @param leader   the player creating it
     * @param teamName the name it should have
     * @param tag      the short tag shown in front of member names
     */
    public static void create(Player leader, String teamName, String tag) {
        String problem = rules().validateName(teamName);
        if (problem != null) {
            leader.sendMessage(ChatColor.RED + "❌ " + problem);
            return;
        }
        if (TeamService.getTeamOf(leader.getUniqueId()) != null) {
            leader.sendMessage(ChatColor.RED + "❌ Du bist schon in einem Team.");
            return;
        }
        if (TeamService.getTeam(teamName) != null) {
            leader.sendMessage(ChatColor.RED + "❌ Diesen Teamnamen gibt es schon.");
            return;
        }
        String sanitized = sanitizeTag(tag);
        if (sanitized.length() > rules().getMaxTagLength()) {
            leader.sendMessage(ChatColor.RED + "❌ Der Team-Tag darf höchstens "
                    + rules().getMaxTagLength() + " Zeichen lang sein.");
            return;
        }

        TeamData team = new TeamData(teamName.trim(), sanitized, leader.getUniqueId());
        team.getSettings().set(TeamSettings.Key.MAX_MEMBERS, rules().getMaxMembersCap());
        TeamService.saveAsync(team, true, result -> {
            if (!result.successful()) {
                leader.sendMessage(ChatColor.RED + "❌ " + result.message());
                return;
            }
            syncScoreboard(result.team());
            syncPlayer(leader);
            leader.sendMessage(ChatColor.GREEN + "✓ Dein Team \"" + result.team().getName() + "\" steht.");
        });
    }

    /**
     * Removes the team for good. The launcher drops its claims and its backpack with it.
     *
     * @param source the player asking for it
     */
    public void disband(Player source) {
        if (!requireLeader(source)) return;
        if (!rules().isAllowDisband()) {
            source.sendMessage(ChatColor.RED + "❌ Teams können auf diesem Server nicht aufgelöst werden.");
            return;
        }
        String teamName = data.getName();
        for (UUID member : data.getMembers()) {
            Player online = Bukkit.getPlayer(member);
            if (online != null) {
                online.sendMessage(ChatColor.RED + "→ Das Team \"" + teamName + "\" wurde aufgelöst.");
            }
        }
        TeamService.deleteAsync(teamName);
        Team scoreboardTeam = scoreboard().getTeam(teamName);
        if (scoreboardTeam != null) scoreboardTeam.unregister();
    }

    /**
     * @param source  the leader
     * @param newName what the team should be called
     */
    public void rename(Player source, String newName) {
        if (!requireLeader(source)) return;
        if (!rules().isAllowRename()) {
            source.sendMessage(ChatColor.RED + "❌ Umbenennen ist auf diesem Server abgeschaltet.");
            return;
        }
        String problem = rules().validateName(newName);
        if (problem != null) {
            source.sendMessage(ChatColor.RED + "❌ " + problem);
            return;
        }
        if (TeamService.getTeam(newName) != null) {
            source.sendMessage(ChatColor.RED + "❌ Diesen Teamnamen gibt es schon.");
            return;
        }
        // the launcher stores teams by name, so a rename is a delete plus a create
        String oldName = data.getName();
        TeamData renamed = copyOf(data);
        renamed.setName(newName.trim());
        renamed.setRevision(0L);
        TeamService.saveAsync(renamed, true, result -> {
            if (!result.successful()) {
                source.sendMessage(ChatColor.RED + "❌ " + result.message());
                return;
            }
            TeamService.deleteAsync(oldName);
            Team old = scoreboard().getTeam(oldName);
            if (old != null) old.unregister();
            syncScoreboard(result.team());
            syncAllOnline();
            source.sendMessage(ChatColor.GREEN + "✓ Dein Team heißt jetzt \"" + newName + "\".");
        });
    }

    /**
     * @param source the leader
     * @param tag    the new short tag
     */
    public void setTag(Player source, String tag) {
        if (!requireLeader(source)) return;
        String sanitized = sanitizeTag(tag);
        if (sanitized.isEmpty() || sanitized.length() > rules().getMaxTagLength()) {
            source.sendMessage(ChatColor.RED + "❌ Der Tag muss 1 bis "
                    + rules().getMaxTagLength() + " Zeichen lang sein.");
            return;
        }
        change(source, team -> team.setTag(sanitized), "Tag geändert.");
    }

    /**
     * @param teamColor the colour to use
     * @param source    the leader
     */
    public void setTeamColor(TeamColor teamColor, Player source) {
        if (teamColor == null || !requireLeader(source)) return;
        change(source, team -> team.setColor(teamColor.getColor().name()), "Farbe geändert.");
    }

    /**
     * Hands the team to somebody else. The old leader stays a normal member.
     *
     * @param source the current leader
     * @param target the member that should take over
     */
    public void transferLeadership(Player source, UUID target) {
        if (!requireLeader(source)) return;
        if (target == null || !data.hasMember(target)) {
            source.sendMessage(ChatColor.RED + "❌ Dieser Spieler ist nicht in deinem Team.");
            return;
        }
        if (target.equals(source.getUniqueId())) {
            source.sendMessage(ChatColor.RED + "❌ Du bist bereits der Anführer.");
            return;
        }
        change(source, team -> team.setLeader(target), "Das Team wurde übergeben.");
        Player newLeader = Bukkit.getPlayer(target);
        if (newLeader != null) {
            newLeader.sendMessage(ChatColor.GREEN + "✓ Du führst jetzt das Team \"" + getName() + "\".");
        }
    }

    /* ------------------------------------------------------------------ membership */

    /**
     * Invites a player. Whether a normal member may do this is up to the team's own settings.
     *
     * @param sender     the player inviting
     * @param inviteName the name of the player to invite
     */
    public void invitePlayer(Player sender, String inviteName) {
        if (!exists()) return;
        if (!mayInvite(sender)) {
            sender.sendMessage(ChatColor.RED + "❌ Nur der Anführer darf einladen.");
            return;
        }
        if (data.getFreeSlots() <= 0 || getPlayerAmount() >= getMaxPlayerAmount()) {
            sender.sendMessage(ChatColor.RED + "❌ Dein Team ist voll.");
            return;
        }
        Player invited = Bukkit.getPlayerExact(inviteName);
        if (invited == null) {
            sender.sendMessage(ChatColor.RED + "❌ \"" + inviteName + "\" ist nicht online.");
            return;
        }
        if (invited.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "❌ Dich selbst brauchst du nicht einzuladen.");
            return;
        }
        if (TeamService.getTeamOf(invited.getUniqueId()) != null) {
            sender.sendMessage(ChatColor.RED + "❌ \"" + inviteName + "\" ist schon in einem Team.");
            return;
        }
        NamespacedKey key = NamespacedKey.fromString(INVITE_KEY);
        if (invited.getPersistentDataContainer().has(key)) {
            sender.sendMessage(ChatColor.RED + "❌ \"" + inviteName + "\" hat schon eine offene Einladung.");
            return;
        }
        invited.getPersistentDataContainer().set(key, PersistentDataType.STRING, data.getName());

        TextComponent invite = Component.text(ChatColor.BLUE + "→ " + sender.getName()
                + " lädt dich in das Team \"" + data.getName() + "\" ein.\n");
        TextComponent accept = Component.text(ChatColor.GREEN + "[ ✓ Annehmen ] ")
                .clickEvent(ClickEvent.runCommand("/cteam invite accept"));
        TextComponent reject = Component.text(ChatColor.RED + "[ ❌ Ablehnen ]")
                .clickEvent(ClickEvent.runCommand("/cteam invite reject"));
        invited.sendMessage(invite.append(accept).append(reject));
        sender.sendMessage(ChatColor.GREEN + "✓ Einladung an \"" + inviteName + "\" geschickt.");
    }

    /**
     * @param player the player that accepted an invite, or that joins an open team
     */
    public void addPlayer(Player player) {
        if (!exists()) {
            player.sendMessage(ChatColor.RED + "❌ Dieses Team gibt es nicht mehr.");
            return;
        }
        if (TeamService.getTeamOf(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "❌ Du bist schon in einem Team.");
            return;
        }
        if (getPlayerAmount() >= getMaxPlayerAmount()) {
            player.sendMessage(ChatColor.RED + "❌ Das Team ist voll.");
            return;
        }
        change(player, team -> team.getMembers().add(player.getUniqueId()),
                "Du bist jetzt im Team \"" + getName() + "\".", team -> {
                    syncPlayer(player);
                    if (team.getSettings().getFlag(TeamSettings.Key.ANNOUNCE_JOINS)) {
                        broadcast(team, ChatColor.GREEN + "→ " + player.getName() + " ist dem Team beigetreten.",
                                player.getUniqueId());
                    }
                });
    }

    /**
     * @param player the player leaving on their own
     */
    public void removePlayer(Player player) {
        if (!exists()) return;
        if (data.isLeader(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED
                    + "❌ Als Anführer musst du das Team erst übergeben oder auflösen.");
            return;
        }
        change(player, team -> team.getMembers().remove(player.getUniqueId()),
                "Du hast das Team verlassen.", team -> {
                    clearScoreboard(player);
                    if (team.getSettings().getFlag(TeamSettings.Key.ANNOUNCE_JOINS)) {
                        broadcast(team, ChatColor.RED + "→ " + player.getName() + " hat das Team verlassen.",
                                player.getUniqueId());
                    }
                });
    }

    /**
     * @param source the leader
     * @param target the member to remove
     */
    public void kickPlayer(Player source, OfflinePlayer target) {
        if (!requireLeader(source)) return;
        if (target == null || !data.hasMember(target.getUniqueId())) {
            source.sendMessage(ChatColor.RED + "❌ Dieser Spieler ist nicht in deinem Team.");
            return;
        }
        if (data.isLeader(target.getUniqueId())) {
            source.sendMessage(ChatColor.RED + "❌ Den Anführer kannst du nicht entfernen.");
            return;
        }
        change(source, team -> team.getMembers().remove(target.getUniqueId()),
                target.getName() + " wurde entfernt.", team -> {
                    Player online = target.getPlayer();
                    if (online != null) {
                        clearScoreboard(online);
                        online.sendMessage(ChatColor.RED + "→ Du wurdest aus dem Team entfernt.");
                    }
                });
    }

    /* ------------------------------------------------------------------ settings */

    /**
     * Flips one of the team's own switches.
     *
     * @param source the leader
     * @param key    the setting to toggle
     */
    public void toggleSetting(Player source, TeamSettings.Key key) {
        if (!requireLeader(source)) return;
        if (key == TeamSettings.Key.PUBLIC_JOIN && !rules().isAllowPublicJoin()) {
            source.sendMessage(ChatColor.RED + "❌ Offene Teams sind auf diesem Server abgeschaltet.");
            return;
        }
        change(source, team -> team.getSettings().toggle(key),
                key.getLabel() + ": " + (data.getSettings().getFlag(key) ? "aus" : "an"));
    }

    /**
     * @param source the leader
     * @param key    the setting to change
     * @param value  the number it should have
     */
    public void setNumber(Player source, TeamSettings.Key key, int value) {
        if (!requireLeader(source)) return;
        int clamped = key == TeamSettings.Key.MAX_MEMBERS
                ? Math.max(getPlayerAmount(), Math.min(rules().getMaxMembersCap(), value))
                : Math.max(0, value);
        change(source, team -> team.getSettings().set(key, clamped), key.getLabel() + ": " + clamped);
    }

    /* ------------------------------------------------------------------ home */

    /**
     * @param source the leader, setting the team home where they stand
     */
    public void setHome(Player source) {
        if (!requireLeader(source)) return;
        Location at = source.getLocation();
        String home = at.getWorld().getName() + ":" + at.getBlockX() + ":" + at.getBlockY()
                + ":" + at.getBlockZ();
        change(source, team -> team.setHome(home), "Team-Home gesetzt.");
    }

    /**
     * @return where the team home is, or {@code null} if none is set or its world is gone
     */
    public Location getHome() {
        if (data == null || data.getHome() == null) return null;
        String[] parts = data.getHome().split(":");
        if (parts.length != 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Integer.parseInt(parts[1]) + 0.5,
                    Integer.parseInt(parts[2]), Integer.parseInt(parts[3]) + 0.5);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ claims */

    /**
     * @param chunk  the chunk to buy
     * @param player the player paying for it
     */
    public void claimChunk(Chunk chunk, Player player) {
        if (!exists()) {
            player.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return;
        }
        if (!mayClaim(player)) {
            player.sendMessage(ChatColor.RED + "❌ Nur der Anführer darf Chunks claimen.");
            return;
        }
        int limit = rules().getMaxClaimsPerTeam();
        if (limit > 0 && data.getClaims().size() >= limit) {
            player.sendMessage(ChatColor.RED + "❌ Dein Team hat das Maximum von " + limit + " Chunks erreicht.");
            return;
        }
        String owner = ClaimManager.getTeamOfChunk(chunk);
        if (owner != null) {
            player.sendMessage(ChatColor.RED + "❌ Dieser Chunk gehört bereits dem Team \"" + owner + "\".");
            return;
        }
        int cost = getChunkCost();
        if (MoneyHandler.getMoney(player.getUniqueId()) < cost) {
            player.sendMessage(ChatColor.RED + "❌ Du brauchst " + cost + " für diesen Chunk.");
            return;
        }
        if (!MoneyHandler.removeMoney(cost, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Die Bezahlung hat nicht geklappt.");
            return;
        }
        change(player, team -> ClaimManager.addClaim(team, chunk),
                "Chunk für " + cost + " geclaimt.", team -> {
                }, () -> MoneyHandler.addMoney(cost, player.getUniqueId()));
    }

    /**
     * @param chunk  the chunk to give up
     * @param player the player doing it, who gets the money back
     */
    public void unclaimChunk(Chunk chunk, Player player) {
        if (!exists()) return;
        if (!mayClaim(player)) {
            player.sendMessage(ChatColor.RED + "❌ Nur der Anführer darf Chunks freigeben.");
            return;
        }
        if (!data.getClaims().contains(ClaimManager.keyOf(chunk))) {
            player.sendMessage(ChatColor.RED + "❌ Dieser Chunk gehört deinem Team nicht.");
            return;
        }
        int refund = rules().claimCost(Math.max(0, data.getClaims().size() - 1));
        change(player, team -> ClaimManager.removeClaim(team, chunk),
                "Chunk freigegeben, " + refund + " zurück.",
                team -> MoneyHandler.addMoney(refund, player.getUniqueId()));
    }

    /**
     * @return what the next chunk costs this team
     */
    public int getChunkCost() {
        return rules().claimCost(data == null ? 0 : data.getClaims().size());
    }

    /* ------------------------------------------------------------------ scoreboard */

    private static Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    /**
     * Makes the scoreboard show the team the way it is stored, so tags and colours match everywhere.
     *
     * @param team the team to mirror
     */
    public static void syncScoreboard(TeamData team) {
        if (team == null || team.getName() == null) return;
        Team scoreboardTeam = scoreboard().getTeam(team.getName());
        if (scoreboardTeam == null) scoreboardTeam = scoreboard().registerNewTeam(team.getName());
        if (team.getTag() != null && !team.getTag().isBlank()) {
            scoreboardTeam.setPrefix("[" + team.getTag() + "] ");
        }
        try {
            scoreboardTeam.setColor(ChatColor.valueOf(team.getColor()));
        } catch (IllegalArgumentException e) {
            scoreboardTeam.setColor(ChatColor.WHITE);
        }
        scoreboardTeam.setAllowFriendlyFire(team.getSettings().getFlag(TeamSettings.Key.FRIENDLY_FIRE));
    }

    /**
     * Puts a player into the scoreboard team they belong to, and takes them out of any other.
     *
     * @param player the player to place
     */
    public static void syncPlayer(Player player) {
        TeamData team = TeamService.getTeamOf(player.getUniqueId());
        if (team == null) {
            clearScoreboard(player);
            return;
        }
        syncScoreboard(team);
        Team scoreboardTeam = scoreboard().getTeam(team.getName());
        if (scoreboardTeam != null && !scoreboardTeam.hasPlayer(player)) {
            clearScoreboard(player);
            scoreboardTeam.addPlayer(player);
        }
    }

    /**
     * Mirrors every team onto the scoreboard and places everybody that is online.
     */
    public static void syncAllOnline() {
        for (TeamData team : TeamService.getTeams()) syncScoreboard(team);
        for (Player player : Bukkit.getOnlinePlayers()) syncPlayer(player);
    }

    private static void clearScoreboard(Player player) {
        Team current = scoreboard().getPlayerTeam(player);
        if (current != null) current.removePlayer(player);
    }

    /* ------------------------------------------------------------------ helpers */

    private boolean requireLeader(Player source) {
        if (!exists()) {
            source.sendMessage(ChatColor.RED + "❌ Du bist in keinem Team.");
            return false;
        }
        if (!data.isLeader(source.getUniqueId())) {
            source.sendMessage(ChatColor.RED + "❌ Das darf nur der Teamanführer.");
            return false;
        }
        return true;
    }

    private boolean mayInvite(Player source) {
        return data.isLeader(source.getUniqueId())
                || (data.hasMember(source.getUniqueId())
                && data.getSettings().getFlag(TeamSettings.Key.MEMBERS_MAY_INVITE));
    }

    private boolean mayClaim(Player source) {
        return data.isLeader(source.getUniqueId())
                || (data.hasMember(source.getUniqueId())
                && data.getSettings().getFlag(TeamSettings.Key.MEMBERS_MAY_CLAIM));
    }

    private void change(Player source, Consumer<TeamData> edit, String success) {
        change(source, edit, success, team -> {
        }, null);
    }

    private void change(Player source, Consumer<TeamData> edit, String success, Consumer<TeamData> after) {
        change(source, edit, success, after, null);
    }

    /**
     * Applies a change and sends it to the launcher.
     * <p>
     * The edit is made on a copy, so a refused write leaves the local view untouched instead of showing a
     * change that never happened.
     *
     * @param source   who to tell about the outcome
     * @param edit     what to change
     * @param success  what to say when it worked
     * @param after    what else to do when it worked
     * @param rollback what to undo when it did not, for changes that already cost something
     */
    private void change(Player source, Consumer<TeamData> edit, String success, Consumer<TeamData> after,
                        Runnable rollback) {
        if (!exists()) return;
        TeamData edited = copyOf(data);
        edit.accept(edited);
        TeamService.saveAsync(edited, false, result -> {
            if (!result.successful()) {
                source.sendMessage(ChatColor.RED + "❌ " + result.message());
                if (rollback != null) rollback.run();
                return;
            }
            data = result.team();
            syncScoreboard(data);
            if (after != null) after.accept(data);
            source.sendMessage(ChatColor.GREEN + "✓ " + success);
        });
    }

    /**
     * @param team the team to copy
     * @return a copy that can be edited without touching the shared local view
     */
    private static TeamData copyOf(TeamData team) {
        TeamData copy = new TeamData();
        copy.setName(team.getName());
        copy.setTag(team.getTag());
        copy.setColor(team.getColor());
        copy.setLeader(team.getLeader());
        copy.getMembers().clear();
        copy.getMembers().addAll(team.getMembers());
        copy.setCreatedAt(team.getCreatedAt());
        copy.setHome(team.getHome());
        copy.setRevision(team.getRevision());
        copy.getClaims().addAll(team.getClaims());
        copy.setSettings(TeamSettings.fromMap(team.getSettings().asMap()));
        return copy;
    }

    /**
     * @param team    the team to talk to
     * @param message what to say
     * @param except  a member to leave out, may be {@code null}
     */
    private static void broadcast(TeamData team, String message, UUID except) {
        for (UUID member : team.getMembers()) {
            if (member.equals(except)) continue;
            Player online = Bukkit.getPlayer(member);
            if (online != null) online.sendMessage(message);
        }
    }

    /**
     * @param name the name of a player
     * @return their uuid, looked up at mojang when they are not online
     */
    public static UUID uuidOf(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        return UUIDFetcher.findUUIDByName(name, true);
    }

    private static String sanitizeTag(String input) {
        if (input == null) return "";
        return input.toUpperCase().replace("[", "").replace("]", "").replace(" ", "").trim();
    }
}
