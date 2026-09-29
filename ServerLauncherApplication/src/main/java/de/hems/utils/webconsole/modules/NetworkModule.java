package de.hems.utils.webconsole.modules;

import de.hems.Main;
import de.hems.types.restart.RestartMode;
import de.hems.types.restart.RestartStatus;
import de.hems.types.server.CapacityData;
import de.hems.types.server.MemoryAdviceData;
import de.hems.utils.restart.RestartScheduler;
import de.hems.utils.server.MemoryWatch;
import de.hems.utils.webconsole.ApiContext;
import de.hems.utils.webconsole.WebModule;
import de.hems.utils.webconsole.WebServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The network as a whole: which code it runs, how much memory it has left, and {@code /neustart} - restart,
 * update or shut down at a set time - without having to be in the game.
 */
public class NetworkModule implements WebModule {

    /** The version does not change while the launcher runs, so it is asked for once. */
    private JSONObject version;

    @Override
    public String getId() {
        return "network";
    }

    @Override
    public String getTitle() {
        return "Netzwerk";
    }

    @Override
    public String getDescription() {
        return "Neustart, Update und Herunterfahren des ganzen Netzwerks, Stand des Codes und Arbeitsspeicher.";
    }

    @Override
    public void register(WebServer server) {
        server.get("/api/network", this::status);
        server.post("/api/network/restart", this::schedule);
        server.post("/api/network/restart/cancel", this::cancel);
    }

    private void status(ApiContext ctx) {
        RestartStatus restart = scheduler().getStatus();
        JSONObject restartJson = new JSONObject()
                .put("scheduled", restart.isScheduled())
                .put("secondsLeft", restart.getSecondsLeft())
                .put("at", restart.getAt())
                .put("lastUpdate", restart.getLastUpdate() == null ? JSONObject.NULL : restart.getLastUpdate());
        if (restart.isScheduled()) {
            restartJson.put("mode", restart.getMode().name())
                    .put("modeTitle", restart.getMode().getTitle())
                    .put("requestedBy", restart.getRequestedBy());
        }

        JSONObject json = new JSONObject()
                .put("restart", restartJson)
                .put("version", version())
                .put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000L)
                .put("maxMinutes", RestartScheduler.MAX_MINUTES);

        MemoryWatch watch = Main.getInstance().getMemoryWatch();
        if (watch != null) {
            CapacityData capacity = watch.snapshot();
            JSONArray advice = new JSONArray();
            for (MemoryAdviceData entry : capacity.getAdvice()) {
                advice.put(new JSONObject()
                        .put("server", entry.getServer())
                        .put("allocated", entry.getAllocatedMB())
                        .put("peak", entry.getPeakUsedMB())
                        .put("suggested", entry.getSuggestedMB())
                        .put("freed", entry.getFreedMB()));
            }
            json.put("memory", new JSONObject()
                    .put("machine", capacity.getTotalMachineMB())
                    .put("reserve", capacity.getReserveMB())
                    .put("budget", capacity.getBudgetMB())
                    .put("allocated", capacity.getAllocatedMB())
                    .put("free", capacity.getFreeMB())
                    .put("refusedRecently", capacity.getRefusedRecently())
                    .put("refusedTotal", capacity.getRefusedTotal())
                    .put("advice", advice));
        }
        ctx.ok(json);
    }

    private void schedule(ApiContext ctx) {
        RestartMode mode = RestartMode.byWord(ctx.string("mode", ""));
        if (mode == null) {
            ctx.error(400, "Unbekannte Art: neustart, update oder aus.");
            return;
        }
        int minutes = ctx.integer("minutes", -1);
        String error = scheduler().schedule(minutes, mode, "Web: " + ctx.session().getUsername());
        if (error != null) {
            ctx.error(400, error);
            return;
        }
        ctx.ok(mode.getTitle() + (minutes == 0 ? " jetzt." : " in " + minutes + " Minuten geplant."));
    }

    private void cancel(ApiContext ctx) {
        String error = scheduler().cancel("Web: " + ctx.session().getUsername());
        if (error != null) {
            ctx.error(409, error);
            return;
        }
        ctx.ok("Abgesagt.");
    }

    private static RestartScheduler scheduler() {
        return Main.getInstance().getRestartScheduler();
    }

    /**
     * @return the commit the launcher runs on, as far as git knows it
     */
    private synchronized JSONObject version() {
        if (version == null) {
            version = new JSONObject()
                    .put("commit", git("rev-parse", "--short", "HEAD"))
                    .put("branch", git("rev-parse", "--abbrev-ref", "HEAD"))
                    .put("subject", git("log", "-1", "--format=%s"))
                    .put("date", git("log", "-1", "--format=%cI"));
        }
        return version;
    }

    /**
     * @param args what to ask git
     * @return the first line of its answer, or an empty string when git is not there or says nothing
     */
    private static String git(String... args) {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            if (process.exitValue() != 0) return "";
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            int newline = out.indexOf('\n');
            return newline < 0 ? out : out.substring(0, newline);
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}
