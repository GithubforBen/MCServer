package de.hems.utils;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;

/**
 * Checks that a store file is never read as empty when it is broken, and never left half written. Run
 * like {@code RewardCheck}; exits non-zero when something is wrong.
 */
public final class YamlFilesCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("yaml").toFile();

        File fresh = new File(dir, "sub/money.yml");
        YamlConfiguration created = YamlFiles.load(fresh);
        check("a missing file is created", fresh.isFile() && created.getKeys(false).isEmpty());

        created.set("balances.abc", 500);
        YamlFiles.save(created, fresh);
        check("what was saved comes back", YamlFiles.load(fresh).getInt("balances.abc") == 500);
        check("no temporary file is left", !new File(dir, "sub/money.yml.tmp").exists());

        File broken = new File(dir, "awards.yml");
        Files.writeString(broken.toPath(), "awards:\n  a: [unclosed\n    b: : :\n");
        boolean refused = false;
        try {
            YamlFiles.load(broken);
        } catch (IllegalStateException e) {
            refused = true;
        }
        check("a broken file stops the start", refused);
        String[] aside = dir.list((d, name) -> name.startsWith("awards.yml.broken-"));
        check("and is copied aside", aside != null && aside.length == 1);
        check("and is left as it was", Files.readString(broken.toPath()).contains("unclosed"));

        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            return;
        }
        failed++;
        System.out.println("FAIL " + what);
    }
}
