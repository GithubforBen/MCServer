package de.hems.utils.event;

import de.hems.types.event.AwardData;
import de.hems.types.event.EventData;
import de.hems.types.event.EventResultData;
import de.hems.types.event.EventRewards;
import de.hems.types.event.EventSetting;
import de.hems.types.event.EventStanding;
import de.hems.types.event.EventType;
import de.hems.types.event.HungerGamesSettings;
import de.hems.types.event.PrizeData;
import de.hems.types.event.RewardRule;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Checks the reward rules, the settings descriptors and the stores they are kept in.
 * <p>
 * Like the poker checks, a class with a main rather than a test framework. From the repository root:
 * <pre>
 * mvn -q -pl ServerLauncherApplication -am install -DskipTests
 * mvn -q -pl ServerLauncherApplication dependency:build-classpath -Dmdep.outputFile=cp.txt
 * javac -cp "$(cat ServerLauncherApplication/cp.txt):ServerLauncherApplication/target/classes" \
 *       -d /tmp/rc ServerLauncherApplication/src/test/java/de/hems/utils/event/RewardCheck.java
 * java -cp "$(cat ServerLauncherApplication/cp.txt):ServerLauncherApplication/target/classes:/tmp/rc" \
 *      de.hems.utils.event.RewardCheck
 * </pre>
 * Exits non-zero when something is wrong.
 */
public final class RewardCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        rules();
        legacy();
        evaluate();
        settings();
        storeKeepsDottedKeys();
        payoutAndResults();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void rules() {
        RewardRule first = RewardRule.place(1, new PrizeData(500).withItem("DIAMOND", 3));
        check("#1 reads as #1", first.describeWho(), "#1");
        RewardRule back = RewardRule.parse(first.serialize());
        check("a rule survives being written out", back.serialize(), first.serialize());
        check("its prize survives too", back.getPrize().getItems().get("DIAMOND"), 3);

        RewardRule top = RewardRule.places(1, 10, new PrizeData(10));
        check("top ten reads as a range", top.describeWho(), "Platz 1-10");
        RewardRule tail = RewardRule.places(10, RewardRule.OPEN_END, new PrizeData(10));
        check("from ten on reads as such", tail.describeWho(), "ab Platz 10");
        check("from ten on takes place 10", tail.matches(new EventStanding(UUID.randomUUID(), 10, 0)), true);
        check("from ten on takes place 57", tail.matches(new EventStanding(UUID.randomUUID(), 57, 0)), true);
        check("from ten on skips place 9", tail.matches(new EventStanding(UUID.randomUUID(), 9, 0)), false);
        check("a placing never goes to the unranked",
                tail.matches(new EventStanding(UUID.randomUUID(), EventStanding.UNRANKED, 0)), false);
        check("top ten skips place 11", top.matches(new EventStanding(UUID.randomUUID(), 11, 0)), false);

        RewardRule kills = RewardRule.kills(5, new PrizeData(1));
        check("kills reads as such", kills.describeWho(), "ab 5 Kills");
        check("five kills is enough", kills.matches(new EventStanding(UUID.randomUUID(), 0, 5)), true);
        check("four is not", kills.matches(new EventStanding(UUID.randomUUID(), 0, 4)), false);
        check("kill rule survives", RewardRule.parse(kills.serialize()).getKills(), 5);

        RewardRule upsideDown = RewardRule.places(8, 3, new PrizeData(1));
        check("a range the wrong way round is put right", upsideDown.getTo(), 8);
        check("text without a who is no rule", RewardRule.parse("money=100"), null);
        check("a broken who is no rule", RewardRule.parse("who=place:x;money=5"), null);
    }

    private static void legacy() {
        EventData event = new EventData("Alt", EventType.UHC_DRAGON, 0, 1);
        PrizeData.setPlace(event, 1, new PrizeData(300));
        PrizeData.setPlace(event, 3, new PrizeData(100));
        PrizeData.setParticipation(event, new PrizeData(20));
        List<RewardRule> rules = EventRewards.of(event);
        check("old prizes are read as rules", rules.size(), 3);
        check("first old prize is #1", rules.get(0).describeWho(), "#1");
        check("second old prize is #3", rules.get(1).describeWho(), "#3");
        check("third is participation", rules.get(2).getCondition(), RewardRule.Condition.PARTICIPATION);

        rules.remove(0);
        EventRewards.set(event, rules);
        check("writing drops the old keys", event.getSetting("prize.place.1", null), null);
        check("and writes the new ones", EventRewards.of(event).size(), 2);
        EventRewards.set(event, new ArrayList<>());
        check("deleting the last rule does not bring the old ones back", EventRewards.of(event).size(), 0);
    }

    private static void evaluate() {
        EventData event = new EventData("HG", EventType.HUNGER_GAMES, 0, 1);
        EventRewards.set(event, List.of(
                RewardRule.place(1, new PrizeData(500)),
                RewardRule.places(2, 3, new PrizeData(200)),
                RewardRule.kills(3, new PrizeData(50)),
                RewardRule.participation(new PrizeData(10)),
                RewardRule.place(4, new PrizeData())));
        UUID winner = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        List<EventStanding> standings = List.of(
                new EventStanding(winner, 1, 4),
                new EventStanding(second, 2, 0),
                new EventStanding(last, 3, 3));
        List<EventRewards.Earned> earned = EventRewards.evaluate(event, standings);
        check("winner gets #1, kills and participation", count(earned, winner), 3);
        check("second gets the range and participation", count(earned, second), 2);
        check("third gets range, kills and participation", count(earned, last), 3);
        check("an empty prize pays nobody", earned.size(), 8);
        check("a kill award is titled by kills", titleOf(earned, last, RewardRule.Condition.KILLS), "3 Kills");
        check("a place award is titled by place", titleOf(earned, second, RewardRule.Condition.PLACE), "2. Platz");

        EventData race = new EventData("UHC", EventType.UHC_DRAGON, 0, 1);
        EventRewards.set(race, List.of(RewardRule.kills(1, new PrizeData(50))));
        check("a kill rule on an event without kills pays nobody",
                EventRewards.evaluate(race, List.of(new EventStanding(winner, 1, 9))).size(), 0);
    }

    private static void settings() {
        EventData event = new EventData("HG", EventType.HUNGER_GAMES, 0, 1);
        HungerGamesSettings hg = new HungerGamesSettings(event);
        hg.applyDefaults();
        check("defaults are written", event.getSetting(HungerGamesSettings.BORDER_START, null), "500");
        EventSetting border = HungerGamesSettings.SETTINGS.get(2);
        border.step(event, true);
        check("a step goes to the next preset", hg.getBorderStart(), 750);
        border.step(event, false);
        border.step(event, false);
        check("and back past where it was", hg.getBorderStart(), 300);
        event.setSetting(HungerGamesSettings.BORDER_START, "640");
        border.step(event, true);
        check("a value off the list snaps to the nearest", hg.getBorderStart(), 750);
        EventSetting drops = HungerGamesSettings.SETTINGS.get(6);
        event.setSetting(HungerGamesSettings.DROP_MINUTES, "0");
        check("zero reads as its label", drops.display(event), "aus");
        EventSetting glow = HungerGamesSettings.SETTINGS.get(7);
        glow.step(event, true);
        check("a toggle flips", hg.isShowdownGlow(), false);
        check("the team size stays one until teams exist", hg.getTeamSize(), 1);
    }

    private static void storeKeepsDottedKeys() throws Exception {
        File file = File.createTempFile("events", ".yml");
        file.delete();
        EventStore store = new EventStore(file);
        EventData event = new EventData("Poker", EventType.POKER, 1000, 2000);
        event.setSetting("poker.buy-in", "500");
        EventRewards.set(event, List.of(RewardRule.place(1, new PrizeData(100))));
        store.put(event, true);
        EventData reloaded = new EventStore(file).getEvent(event.getId());
        check("a dotted setting survives a restart", reloaded.getSetting("poker.buy-in", null), "500");
        check("a reward survives a restart", EventRewards.of(reloaded).size(), 1);
        file.delete();
    }

    private static void payoutAndResults() throws Exception {
        File awardsFile = File.createTempFile("awards", ".yml");
        awardsFile.delete();
        File resultsFile = File.createTempFile("results", ".yml");
        resultsFile.delete();
        AwardStore awards = new AwardStore(awardsFile);
        EventResultStore results = new EventResultStore(resultsFile);

        EventData event = new EventData("HG", EventType.HUNGER_GAMES, 0, 1);
        EventRewards.set(event, List.of(RewardRule.place(1, new PrizeData(500)),
                RewardRule.kills(2, new PrizeData(50))));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        EventResultData winner = new EventResultData(event.getId(), a, "A");
        winner.setPlace(1);
        winner.setKills(2);
        EventResultData loser = new EventResultData(event.getId(), b, "B");
        loser.setPlace(2);
        results.put(List.of(winner, loser));

        EventResultStore reread = new EventResultStore(resultsFile);
        check("result lines survive a restart", reread.getRowsOf(event.getId()).size(), 2);

        List<EventStanding> standings = new ArrayList<>();
        for (EventResultData row : reread.getRowsOf(event.getId())) standings.add(row.toStanding());
        check("two rewards are put aside", RewardPayout.pay(awards, event, standings), 2);
        List<AwardData> won = new AwardStore(awardsFile).getUnclaimed(a);
        check("the winner has both waiting after a restart", won.size(), 2);
        check("with the titles the player reads", won.get(0).getPlaceTitle().equals("1. Platz")
                || won.get(0).getPlaceTitle().equals("2 Kills"), true);
        check("the loser has nothing", awards.getUnclaimed(b).size(), 0);

        results.discard(event.getId());
        check("discarded lines are gone after a restart",
                new EventResultStore(resultsFile).getRowsOf(event.getId()).size(), 0);
        awardsFile.delete();
        resultsFile.delete();
    }

    private static int count(List<EventRewards.Earned> earned, UUID player) {
        int count = 0;
        for (EventRewards.Earned entry : earned) {
            if (entry.standing().getPlayer().equals(player)) count++;
        }
        return count;
    }

    private static String titleOf(List<EventRewards.Earned> earned, UUID player, RewardRule.Condition condition) {
        for (EventRewards.Earned entry : earned) {
            if (entry.standing().getPlayer().equals(player) && entry.rule().getCondition() == condition) {
                return entry.title();
            }
        }
        return null;
    }

    private static void check(String what, Object actual, Object expected) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
            return;
        }
        failed++;
        System.out.println("FAIL " + what + ": expected " + expected + ", got " + actual);
    }
}
