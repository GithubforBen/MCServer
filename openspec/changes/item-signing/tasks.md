# Tasks

## 1. Signing

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/signing/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/signing/**
  - Survival/src/main/resources/plugin.yml
  - Survival/src/main/java/de/schnorrenbergers/survival/Survival.java
depends: []
scenarios:
  - item-signing#signing-a-stack
  - item-signing#empty-hand
  - item-signing#signed-ten-times
  - item-signing#signing-twice
  - item-signing#someone-else-s-signature
  - item-signing#gadget-item
agent: implementer
```

- [x] 1.1 Write the signing rules as pure code with `SigningRulesCheck`
  - Done when: `SigningRulesCheck` runs and exits 0
- [x] 1.2 Add `/signItem` writing the signature and the lore line
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [x] 1.3 Accept `/signItem` in any letter case; the in-game run found that only `/signitem` worked
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; the case rule has a pure check; the game run shows `/signItem` working

## 2. Keep signed items out of recipes

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/Survival.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/signing/guard/**
depends: [1]
scenarios:
  - item-signing#crafting-table
  - item-signing#crafter-skips-signed-items
  - item-signing#furnace
  - item-signing#anvil-left-and-right
agent: implementer
```

- [x] 2.1 Block signed items at the crafting table, the crafter and the smithing table
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [x] 2.2 Block signed items in furnaces and as the anvil sacrifice
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [x] 2.3 Keep signed items out of lab brews, on top and in the bottom slots, and wire the guards into `Survival.java`
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and the lab checks still exit 0

## 3. Docs and recipe

```yaml
files:
  - ServerLauncherApplication/src/main/resources/discord-info/spieler.md
  - README.md
  - testing/README.md
  - testing/item-signing/**
depends: [2]
scenarios: []
agent: implementer
```

- [x] 3.1 Document signing in the README and add a recipe to `testing/README.md`
  - Done when: both files describe the feature
- [x] 3.2 Add `testing/item-signing/recipe.sh` naming, with a `spec: item-signing#<scenario>` comment, every scenario no check file covers
  - Done when: `specfold trace --change item-signing` reports 0 uncovered
- [x] 3.3 Describe the feature for players in `spieler.md`, under "Survival"
  - Done when: `InfoTextCheck` runs and exits 0

## 4. In-game check

```yaml
files:
  - testing/item-signing/**
depends: [3]
scenarios:
  - item-signing#selling-signed-goods
  - item-signing#unsigned-is-not-signed
  - item-signing#buyback-of-signed-goods
agent: implementer
```

- [x] 4.1 The orchestrator runs the recipe on a local network with real clients, including shop sales and buyback of signed goods
  - Done when: every scenario of `item-signing` was seen working in game, or the failure is reported

## 5. Knowledge

```yaml
files: [knowledge/**]
depends: [4]
scenarios: []
agent: knowledge-curator
```

- [ ] 5.1 Write the concepts listed in knowledge.md
  - Done when: `specfold validate` exits 0

## Workflow follow-up

- Archive the change, then run `specfold sync item-signing` and commit the result in the same PR.
