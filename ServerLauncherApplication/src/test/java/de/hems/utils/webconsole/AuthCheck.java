package de.hems.utils.webconsole;

import de.hems.utils.Configuration;
import de.hems.utils.webconsole.auth.AuthService;
import de.hems.utils.webconsole.auth.LoginResult;
import de.hems.utils.webconsole.auth.Session;
import de.hems.utils.webconsole.auth.Totp;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

/**
 * Checks the logins of the website: "Angemeldet bleiben", logins that outlive a restart, and that a changed
 * password ends them.
 * <p>
 * Like the other checks, a class with a main. It works in the current directory, because the launcher config
 * is always {@code ./main-config.yml} - so run it from an empty one:
 * <pre>
 * mvn -q -pl ServerLauncherApplication -am install -DskipTests
 * mvn -q -pl ServerLauncherApplication dependency:build-classpath -Dmdep.outputFile=cp.txt
 * javac -cp "$(cat ServerLauncherApplication/cp.txt):ServerLauncherApplication/target/classes" \
 *       -d /tmp/ac ServerLauncherApplication/src/test/java/de/hems/utils/webconsole/AuthCheck.java
 * mkdir -p /tmp/ac-run && cd /tmp/ac-run && java -cp "...:/tmp/ac" de.hems.utils.webconsole.AuthCheck
 * </pre>
 * Exits non-zero when something is wrong.
 */
public final class AuthCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File config = new File("./main-config.yml");
        File sessions = new File("./web-sessions.yml");
        if (config.exists() || sessions.exists()) {
            System.out.println("Run this in an empty directory - it writes main-config.yml and web-sessions.yml.");
            System.exit(2);
        }
        Files.writeString(config.toPath(), "web:\n  grace-period-seconds: 0\n  remember-days: 7\n");
        try {
            run(sessions);
        } finally {
            config.delete();
            sessions.delete();
            new File("./web-sessions.yml.tmp").delete();
            new File("./main-config.yml.tmp").delete();
        }
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void run(File sessions) throws Exception {
        Configuration configuration = new Configuration();
        AuthService auth = new AuthService(configuration);
        auth.persistSessions(sessions);
        String secretA = Totp.generateSecret();
        String secretB = Totp.generateSecret();
        auth.saveAccount("anna", "passwort-anna", secretA);
        auth.saveAccount("bert", "passwort-bert", secretB);

        LoginResult plain = auth.login("anna", "passwort-anna", code(auth, secretA), "1", false);
        check("a login works", plain.getStatus().isSuccess(), true);
        Session plainSession = plain.getSession();
        check("without the tick it is not remembered", plainSession.isRemember(), false);
        long idle = plainSession.getExpiresAt() - System.currentTimeMillis();
        check("without the tick it runs for the idle timeout", idle > 59 * 60_000L && idle <= 60 * 60_000L, true);

        LoginResult remembered = auth.login("bert", "passwort-bert", code(auth, secretB), "2", true);
        Session rememberedSession = remembered.getSession();
        check("with the tick it is remembered", rememberedSession.isRemember(), true);
        long week = rememberedSession.getExpiresAt() - System.currentTimeMillis();
        check("with the tick it runs for seven days", week > 7 * 86_400_000L - 60_000L && week <= 7 * 86_400_000L, true);
        String rememberedToken = rememberedSession.getToken();
        long fixedEnd = rememberedSession.getExpiresAt();
        Thread.sleep(5);
        check("a remembered session is found", auth.getSession(rememberedToken) != null, true);
        check("and is not pushed back by use", auth.getSession(rememberedToken).getExpiresAt(), fixedEnd);

        String fileText = Files.readString(sessions.toPath());
        check("the token itself is not written down", fileText.contains(rememberedToken), false);
        check("its hash is", fileText.contains(Session.hash(rememberedToken)), true);

        // a restart: a new service reading the same file
        AuthService restarted = new AuthService(new Configuration());
        restarted.persistSessions(sessions);
        Session afterRestart = restarted.getSession(rememberedToken);
        check("the login survives a restart", afterRestart != null, true);
        check("with its csrf token", afterRestart == null ? null : afterRestart.getCsrfToken(),
                rememberedSession.getCsrfToken());
        check("and its end", afterRestart == null ? null : afterRestart.getExpiresAt(), fixedEnd);
        check("an unknown token is no session", restarted.getSession("nonsense"), null);

        // the password of bert changes behind the launcher's back, e.g. by ./admin-passwort.sh
        restarted.saveAccount("bert", "neues-passwort", secretB);
        check("a changed password ends the session", restarted.getSession(rememberedToken), null);

        // anna changes her own password on the website: her session stays, others end
        String plainToken = plainSession.getToken();
        Session annaNow = restarted.getSession(plainToken);
        check("anna is still logged in", annaNow != null, true);
        restarted.saveAccount("anna", "anderes-passwort", secretA);
        restarted.endSessions("anna", annaNow.getId());
        check("the session that changed the password stays", restarted.getSession(plainToken) != null, true);

        restarted.logout(restarted.getSession(plainToken));
        check("a logout ends it", restarted.getSession(plainToken), null);
        AuthService again = new AuthService(new Configuration());
        again.persistSessions(sessions);
        check("and it stays ended after a restart", again.getSession(plainToken), null);

        restarted.deleteAccount("anna");
        check("a deleted account has no sessions", again.getSession(plainToken), null);
    }

    /**
     * @return the current code for a secret, as an authenticator app would show it
     */
    private static String code(AuthService auth, String secret) throws Exception {
        Method decode = Totp.class.getDeclaredMethod("decodeBase32", String.class);
        decode.setAccessible(true);
        Method generate = Totp.class.getDeclaredMethod("generate", byte[].class, long.class);
        generate.setAccessible(true);
        long step = System.currentTimeMillis() / 1000L / auth.getTotp().getPeriodSeconds();
        return (String) generate.invoke(auth.getTotp(), decode.invoke(null, secret), step);
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
