package de.hems.utils.item;

import de.hems.types.admin.ItemData;
import de.hems.types.admin.StashData;
import de.hems.types.event.EventData;
import de.hems.types.event.EventRewards;
import de.hems.types.event.EventType;
import de.hems.types.event.PrizeData;
import de.hems.types.event.RewardRule;
import de.hems.types.item.ItemCatalog;
import de.hems.types.item.ItemSpec;
import de.hems.utils.admin.StashStore;
import de.hems.utils.event.EventForm;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Checks hand made items: their description, how prizes store them, how the website's forms read them, and
 * where the launcher keeps them and the catalog.
 * <p>
 * Like the other checks, a class with a main rather than a test framework. From the repository root:
 * <pre>
 * mvn -q -pl ServerLauncherApplication -am install -DskipTests
 * mvn -q -pl ServerLauncherApplication dependency:build-classpath -Dmdep.outputFile=cp.txt
 * javac -cp "$(cat ServerLauncherApplication/cp.txt):ServerLauncherApplication/target/classes" \
 *       -d /tmp/ic ServerLauncherApplication/src/test/java/de/hems/utils/item/ItemCheck.java
 * java -cp "$(cat ServerLauncherApplication/cp.txt):ServerLauncherApplication/target/classes:/tmp/ic" \
 *      de.hems.utils.item.ItemCheck
 * </pre>
 * Exits non-zero when something is wrong.
 */
public final class ItemCheck {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        spec();
        prizes();
        rewardForm();
        slots();
        stash();
        catalogStore();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static ItemSpec sword() {
        ItemSpec sword = new ItemSpec("minecraft:diamond_sword", 1);
        sword.setName("&6Excalibur; der Echte, =scharf=");
        sword.setLore(List.of("&7Zeile eins", "Zeile, zwei"));
        sword.withEnchantment("Sharpness", 7).withEnchantment("minecraft:unbreaking", 3);
        sword.withAttribute(new ItemSpec.Modifier("generic.attack_damage", 4.5, ItemSpec.Operation.ADD_NUMBER,
                "MAINHAND"));
        sword.setUnbreakable(true);
        sword.setDamage(12);
        return sword;
    }

    private static void spec() throws Exception {
        ItemSpec sword = sword();
        check("the material is spelled the bukkit way", sword.getMaterial(), "DIAMOND_SWORD");
        check("an enchantment gets its namespace", sword.getEnchantments().get("minecraft:sharpness"), 7);
        check("an old attribute name means the new one", sword.getAttributes().get(0).getAttribute(),
                "minecraft:attack_damage");
        check("the slot is lower case", sword.getAttributes().get(0).getSlot(), "mainhand");
        check("it is fine", sword.problem(64), null);
        check("it is not plain", sword.isPlain(), false);

        ItemSpec back = ItemSpec.fromJson(sword.toJson());
        check("json keeps everything", back, sword);
        check("encoding keeps everything", ItemSpec.decode(sword.encode()), sword);
        check("the encoding has none of the separators a prize line uses",
                sword.encode().matches("[A-Za-z0-9_-]+"), true);
        check("garbage does not decode", ItemSpec.decode("@@@"), null);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new PrizeData(5).withItem(sword));
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            PrizeData travelled = (PrizeData) in.readObject();
            check("a prize travels the network with its items", travelled.getItems().get(0), sword);
        }

        ItemSpec tooHigh = sword.copy().withEnchantment("sharpness", 256);
        check("a level above 255 is refused", tooHigh.problem(64) != null, true);
        ItemSpec badKey = new ItemSpec("DIAMOND", 1).withEnchantment("schärfe!", 1);
        check("a key that cannot be a key is refused", badKey.problem(64) != null, true);
        ItemSpec badSlot = new ItemSpec("DIAMOND", 1).withAttribute(
                new ItemSpec.Modifier("armor", 1, ItemSpec.Operation.ADD_NUMBER, "pocket"));
        check("an unknown slot is refused", badSlot.problem(64) != null, true);
        ItemSpec huge = new ItemSpec("DIAMOND", 1).withAttribute(
                new ItemSpec.Modifier("armor", 1e9, ItemSpec.Operation.ADD_NUMBER, "any"));
        check("an absurd attribute amount is refused", huge.problem(64) != null, true);
        check("too many is refused", new ItemSpec("DIAMOND", 65).problem(64) != null, true);
        check("the copy is its own", sword.copy() != sword && sword.copy().equals(sword), true);
        check("it reads well", new ItemSpec("DIAMOND_SWORD", 1).withEnchantment("sharpness", 5).describe(),
                "1x DIAMOND_SWORD (sharpness 5)");
    }

    private static void prizes() {
        PrizeData prize = new PrizeData(100).withItem("DIAMOND", 3).withItem(sword()).withItem("diamond", 2);
        check("plain items of a kind are added up", prize.amountOf("DIAMOND"), 5);
        check("the enchanted one stays its own", prize.getItems().size(), 2);
        String line = prize.serialize();
        check("a plain item stays readable", line.contains("DIAMOND:5"), true);
        PrizeData back = PrizeData.parse(line);
        check("the enchanted sword survives", back.getItems().get(1), sword());
        check("the money survives", back.getMoney(), 100);
        check("the order survives", back.getItems().get(0).getMaterial(), "DIAMOND");

        PrizeData old = PrizeData.parse("money=10;items=DIAMOND:2,IRON_INGOT:5");
        check("an old prize is still read", old.amountOf("IRON_INGOT"), 5);
        PrizeData broken = PrizeData.parse("money=10;items=DIAMOND:2,~@@@,GOLD_INGOT:1");
        check("a broken entry costs only itself", broken.getItems().size(), 2);

        RewardRule rule = RewardRule.place(1, prize);
        RewardRule ruleBack = RewardRule.parse(rule.serialize());
        check("a rule keeps its enchanted prize", ruleBack.getPrize().getItems().get(1), sword());
        check("and who gets it", ruleBack.describeWho(), "#1");

        EventData event = new EventData("Bedwars", EventType.BEDWARS, 0, 1);
        EventRewards.set(event, List.of(rule));
        check("the event keeps it", EventRewards.of(event).get(0).getPrize().getItems().get(1), sword());

        PrizeData twice = new PrizeData().withItem(sword()).withItem(sword());
        check("two identical enchanted items are added up", twice.getItems().get(0).getAmount(), 2);
        check("and not counted as plain", twice.amountOf("DIAMOND_SWORD"), 0);
    }

    private static ItemCatalog catalog() {
        return new ItemCatalog(
                List.of(new ItemCatalog.Material("DIAMOND", false, 64, 0),
                        new ItemCatalog.Material("DIAMOND_SWORD", false, 1, 1561)),
                List.of(new ItemCatalog.Enchantment("minecraft:sharpness", 5, List.of("DIAMOND_SWORD")),
                        new ItemCatalog.Enchantment("minecraft:unbreaking", 3, List.of("DIAMOND_SWORD"))),
                List.of("minecraft:attack_damage", "minecraft:armor"), 1L);
    }

    private static void rewardForm() {
        EventData event = new EventData("Bedwars", EventType.BEDWARS, 0, 1);
        JSONArray rewards = new JSONArray().put(new JSONObject().put("who", "PLACE").put("from", 1).put("to", 1)
                .put("items", new JSONArray().put(sword().toJson())
                        .put(new JSONObject().put("material", "").put("amount", 1))));
        check("an enchanted prize is taken", EventForm.applyRewards(event, rewards, ItemSpecCatalog.of(catalog())),
                null);
        check("and kept", EventRewards.of(event).get(0).getPrize().getItems().get(0), sword());
        check("an empty row is no item", EventRewards.of(event).get(0).getPrize().getItems().size(), 1);
        JSONObject described = EventForm.describeRewards(event).getJSONObject(0).getJSONArray("items")
                .getJSONObject(0);
        check("the form gets the whole item back", described.getJSONArray("enchantments").length(), 2);

        JSONArray unknown = new JSONArray().put(new JSONObject().put("who", "PARTICIPATION")
                .put("items", new JSONArray().put(new ItemSpec("DIAMOND_SWORD", 1)
                        .withEnchantment("minecraft:looting", 3).toJson())));
        check("an enchantment the server does not know is refused",
                EventForm.applyRewards(event, unknown, ItemSpecCatalog.of(catalog())) != null, true);
        check("but taken when there is no catalog to ask",
                EventForm.applyRewards(event, unknown, ItemCatalog::empty), null);

        JSONArray worn = new JSONArray().put(new JSONObject().put("who", "PARTICIPATION")
                .put("items", new JSONArray().put(new JSONObject().put("material", "DIAMOND").put("amount", 1)
                        .put("damage", 3))));
        check("wear on an item that does not wear is refused",
                EventForm.applyRewards(event, worn, ItemSpecCatalog.of(catalog())) != null, true);

        AtomicInteger asked = new AtomicInteger();
        JSONArray noItems = new JSONArray().put(new JSONObject().put("who", "PARTICIPATION").put("money", 5));
        EventForm.applyRewards(event, noItems, () -> {
            asked.incrementAndGet();
            return catalog();
        });
        check("the catalog is not asked for a prize without items", asked.get(), 0);
        JSONArray manyItems = new JSONArray().put(new JSONObject().put("who", "PARTICIPATION").put("items",
                new JSONArray().put(new ItemSpec("DIAMOND", 1).toJson()).put(new ItemSpec("DIAMOND", 2).toJson())));
        EventForm.applyRewards(event, manyItems, () -> {
            asked.incrementAndGet();
            return catalog();
        });
        check("and only once for many", asked.get(), 1);
    }

    private static void slots() {
        JSONObject edited = new JSONObject().put("slot", 3).put("material", "STONE").put("amount", 1)
                .put("modified", true).put("spec", sword().toJson()).put("raw", JSONObject.NULL);
        JSONObject moved = new JSONObject().put("slot", 4).put("material", "DIAMOND").put("amount", 7)
                .put("raw", "AAEC").put("spec", new ItemSpec("DIAMOND", 1).toJson());
        JSONObject outside = new JSONObject().put("slot", 99).put("material", "DIAMOND").put("amount", 1);
        SlotJson.Result read = SlotJson.read(new JSONArray().put(edited).put(moved).put(outside), 41);
        check("slots are read", read.problem(), null);
        check("a slot outside the container is dropped", read.items().size(), 2);
        ItemData first = read.items().get(0);
        check("an edited item is marked", first.isModified(), true);
        check("its material follows the description", first.getMaterial(), "DIAMOND_SWORD");
        ItemData second = read.items().get(1);
        check("a moved item is not marked", second.isModified(), false);
        check("its bytes come along", second.getRawBase64(), "AAEC");
        check("its description follows its amount", second.getSpec().getAmount(), 7);
        check("and it goes back out the same", SlotJson.toJson(first).getJSONObject("spec").getString("name"),
                sword().getName());

        JSONObject wrong = new JSONObject().put("slot", 0).put("modified", true)
                .put("spec", new ItemSpec("DIAMOND", 1).withEnchantment("sharpness", 999).toJson());
        check("an edited item that makes no sense is refused",
                SlotJson.read(new JSONArray().put(wrong), 41).problem() != null, true);
    }

    private static void stash() throws Exception {
        File file = File.createTempFile("stash", ".yml");
        file.deleteOnExit();
        StashStore store = new StashStore(file);
        StashData stash = store.get(StashData.GLOBAL);
        ItemData item = new ItemData();
        item.setSlot(2);
        item.setMaterial("DIAMOND_SWORD");
        item.setAmount(1);
        item.setSpec(sword());
        item.setModified(true);
        StashData changed = new StashData(StashData.GLOBAL, 54, List.of(item), stash.getRevision());
        check("the stash takes it", store.put(changed).successful(), true);

        StashData reloaded = new StashStore(file).get(StashData.GLOBAL);
        check("the description survives a restart", reloaded.getItems().get(0).getSpec(), sword());
        check("and that it still has to be put onto the item", reloaded.getItems().get(0).isModified(), true);
    }

    private static void catalogStore() throws Exception {
        File file = File.createTempFile("catalog", ".json");
        file.delete();
        file.deleteOnExit();
        AtomicInteger asked = new AtomicInteger();
        ItemCatalogStore nobody = new ItemCatalogStore(file, () -> {
            asked.incrementAndGet();
            return null;
        });
        check("without a server and a copy there is nothing", nobody.catalog().isEmpty(), true);
        nobody.catalog();
        check("the network is not asked again right away", asked.get(), 1);

        ItemCatalogStore running = new ItemCatalogStore(file, ItemCheck::catalog);
        check("a running server answers", running.catalog().getMaterials().size(), 2);
        check("which counts as live", running.isLive(), true);
        check("and is written down", Files.exists(file.toPath()), true);

        ItemCatalogStore afterRestart = new ItemCatalogStore(file, () -> null);
        ItemCatalog stored = afterRestart.catalog();
        check("after a restart the copy is used", stored.getEnchantments().get(0).key(), "minecraft:sharpness");
        check("with what each enchantment goes on", stored.getEnchantments().get(0).materials(),
                List.of("DIAMOND_SWORD"));
        check("but it does not count as live", afterRestart.isLive(), false);
    }

    /** A supplier of a fixed catalog, spelled out so the calls above read as what they are. */
    private static final class ItemSpecCatalog {
        static java.util.function.Supplier<ItemCatalog> of(ItemCatalog catalog) {
            return () -> catalog;
        }
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
