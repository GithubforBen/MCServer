# Tasks

## 1. Brew engine

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/main/java/de/schnorrenbergers/survival/Survival.java
depends: []
scenarios:
  - lab-brewing#chunk-unloads-mid-brew
  - lab-brewing#restart-mid-brew
  - lab-brewing#crash-mid-brew
  - lab-brewing#stand-broken-mid-brew
  - lab-brewing#ingredient-taken-out-mid-brew
  - lab-brewing#many-stands-reloading
  - lab-brewing#sugar-cane-through-a-hopper
  - lab-brewing#watching-a-brew
agent: implementer
```

- [x] 1.1 Write the brew state as a pure class with `LabBrewCheck`, covering pause, resume, ingredient loss and double start
  - Done when: `LabBrewCheck` runs and exits 0
- [x] 1.2 Run the own tick on a stand with empty slots, persisted in the block, with a placeholder result
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; the server run is task 2.2
- [x] 1.3 Add the hopper-out rule and the progress arrow for viewers
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [x] 1.4 Keep a stand brewing after a finish while it can brew, and wake it when items leave it, not only when they arrive
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabBrewCheck` still exits 0; the server run in task 2.2 shows it
- [x] 1.5 Make the progress arrow follow the own-tick brew for everyone who has the stand open; the game run showed it empty for the whole brew
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabBrewCheck` still exits 0; the game run shows `lab-brewing#watching-a-brew`
- [x] 1.6 Also send the total brew time, data slot 2, because the client draws the arrow only when brewing ticks and total brewing ticks are both above 0; the game run after 1.5 still showed an empty arrow
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabBrewCheck` still exits 0; the game run shows `lab-brewing#watching-a-brew`

## 2. Prove empty slots on a server

```yaml
files:
  - testing/lab-brewing/proof/**
depends: [1]
scenarios: []
agent: implementer
```

- [x] 2.1 Write `testing/lab-brewing/proof/recipe.sh`, a checklist that prints numbered steps and exits 2, with a `spec: lab-brewing#<scenario>` comment at the step that exercises each scenario of group 1
  - Done when: `specfold trace --change lab-brewing` lists none of group 1's scenarios as uncovered
- [x] 2.2 The orchestrator, not an implementer, runs the proof recipe on a local server: chunk unload, repeated reloads with many stands, restart, `kill -9`, breaking, taking the ingredient, hopper in from above
  - Done when: every case was seen working, or the failure is reported to the user with the glass bottle fallback as the question; the progress arrow needs a client and is shown in task 10.1

## 3. Items and config

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/item/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/LabSettings.java
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/item/**
depends: [2]
scenarios:
  - lab-brewing#placing-is-refused
  - lab-brewing#item-names-the-next-step
agent: implementer
```

- [x] 3.1 Build paste, Kokain, filters, Feenstaub and Pflanzenabfall without a resource pack
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [x] 3.2 Read `configs/lab.yml` with defaults written back
  - Done when: a missing file is created with every default
- [x] 3.3 Refuse placing Pflanzenabfall
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; placing needs a running server, so `lab-brewing#placing-is-refused` is proven in task 10.1

## 4. Crafting

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/craft/**
depends: [3]
scenarios:
  - lab-brewing#crafter-makes-filters
  - lab-brewing#plain-item-does-not-pass-as-a-filter
  - lab-brewing#lab-item-in-a-normal-recipe
  - lab-brewing#bystander-sees-nothing
agent: implementer
```

- [x] 4.1 Register the filter and Feenstaub recipes, undiscovered so nobody gets a notification
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; `lab-brewing#crafter-makes-filters` is proven in task 10.1
- [x] 4.2 Keep lab items out of normal recipes
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; `lab-brewing#lab-item-in-a-normal-recipe` is proven in task 10.1

## 5. Production chain

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/chain/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/chain/**
depends: [3]
scenarios:
  - lab-brewing#three-empty-slots
  - lab-brewing#two-sugar-cane-left
  - lab-brewing#no-free-slot
  - lab-brewing#matching-filter
  - lab-brewing#papierfilter-chances
  - lab-brewing#kohlefilter-chances
  - lab-brewing#seidenfilter-chances
  - lab-brewing#goldfilter-chances
  - lab-brewing#wrong-filter
  - lab-brewing#mixed-tiers
  - lab-brewing#purity-carries-over
  - lab-brewing#simulated-average
  - lab-brewing#waste-blocks-the-slot
  - lab-brewing#automatic-purification
  - lab-brewing#input-is-not-pulled-out
  - lab-brewing#results-are-pulled-out
  - lab-brewing#normal-potion
agent: implementer
```

- [x] 5.1 Write the step table and rolling as pure code with `LabChainCheck`
  - Done when: `LabChainCheck` runs and exits 0
- [x] 5.2 Prove the chain's average with a seeded simulation
  - Done when: `lab-brewing#simulated-average` passes in `LabChainCheck`
- [x] 5.3 Connect sugar cane to the own tick
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; `lab-brewing#three-empty-slots` is proven in task 10.1
- [x] 5.4 Run purifying and Kokain through `BrewingRecipe` and `BrewEvent`
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabChainCheck` still exits 0; `lab-brewing#matching-filter` is proven in task 10.1
- [x] 5.5 Apply the hopper rule to all steps
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; `lab-brewing#automatic-purification` is proven in task 10.1

## 6. Upgrades

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/chain/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/chain/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/upgrade/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/upgrade/**
depends: [5]
scenarios:
  - lab-upgrades#water-cauldron-raises-the-yield
  - lab-upgrades#two-cauldrons-count-once
  - lab-upgrades#empty-cauldron-does-nothing
  - lab-upgrades#lightning-rod-shortens-a-brew
  - lab-upgrades#fully-upgraded
  - lab-upgrades#removing-an-upgrade
  - lab-upgrades#normal-potion-near-an-upgrade
agent: implementer
```

- [x] 6.1 Compute chances from upgrades as pure code with `LabUpgradeCheck`, including the fully upgraded average (about 6,500)
  - Done when: `LabUpgradeCheck` runs and exits 0
- [x] 6.2 Read the blocks around a stand when a brew starts
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabUpgradeCheck` still exits 0; `lab-upgrades#water-cauldron-raises-the-yield` is proven in task 10.1

## 7. Cutting

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/chain/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/chain/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/cutting/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/cutting/**
depends: [6]
scenarios:
  - lab-cutting#adding-an-effect
  - lab-cutting#fourth-ingredient
  - lab-cutting#same-effect-twice
  - lab-cutting#order-does-not-matter
  - lab-cutting#stretching
  - lab-cutting#no-second-stretch
  - lab-cutting#lowest-purity
  - lab-cutting#automatic-cutting
agent: implementer
```

- [x] 7.1 Write cutting and stretching as pure code with `LabCuttingCheck`
  - Done when: `LabCuttingCheck` runs and exits 0
- [x] 7.2 Run them through `BrewingRecipe` and `BrewEvent`, with stacking by sorted effects
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabCuttingCheck` still exits 0; `lab-cutting#automatic-cutting` is proven in task 10.1

## 8. Consumption

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/consume/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/consume/**
depends: [3]
scenarios:
  - lab-consumption#purity-3
  - lab-consumption#cut-in-effect
  - lab-consumption#crash-after-the-effects
  - lab-consumption#logging-out-does-not-skip-it
  - lab-consumption#death-cancels-it
  - lab-consumption#too-soon
  - lab-consumption#hit-during-purity-5
  - lab-consumption#consuming-in-combat
  - lab-consumption#mob-hit-does-not-count
  - lab-consumption#holding-it-does-nothing
agent: implementer
```

- [x] 8.1 Write effect tables and the combat cap as pure code with `LabConsumeCheck`
  - Done when: `LabConsumeCheck` runs and exits 0
- [x] 8.2 Consume on right click, with cooldown and crash, surviving a relog
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; `lab-consumption#logging-out-does-not-skip-it` is proven in task 10.1
- [x] 8.3 Track combat between players and cap the effects
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and `LabConsumeCheck` still exits 0; `lab-consumption#hit-during-purity-5` is proven in task 10.1

## 9. Command and docs

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/brew/LabBrewing.java
  - Survival/src/main/java/de/schnorrenbergers/survival/Survival.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/LabCommand.java
  - Survival/src/main/resources/plugin.yml
  - README.md
  - testing/README.md
  - ServerLauncherApplication/src/main/resources/discord-info/spieler.md
  - testing/lab-brewing/recipe.sh
depends: [4, 6, 7, 8]
scenarios:
  - lab-brewing#asking-for-the-tree
agent: implementer
```

- [x] 9.1 Add `/labor` with the tree
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; `lab-brewing#asking-for-the-tree` is proven in task 10.1
- [x] 9.2 Document the lab in the README, the player info and `testing/README.md`
  - Done when: all three describe the feature
- [x] 9.3 Write `testing/lab-brewing/recipe.sh`, a checklist that prints numbered steps and exits 2, with a `spec: <capability>#<scenario>` comment for every scenario no check file covers
  - Done when: `specfold trace --change lab-brewing` reports 0 uncovered
- [x] 9.4 Wire every lab `init` that is not yet called, such as `LabItemListeners.init`, into `Survival.java`, with one shared `LabSettings`
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes and each lab listener is registered once

## 10. In-game check

```yaml
files:
  - testing/lab-brewing/recipe.sh
depends: [9]
scenarios: []
agent: implementer
```

- [x] 10.1 The orchestrator, not an implementer, runs the recipe on a local network with real clients, including a whole automated line from sugar cane to consumed Kokain that no player touches
  - Done when: every scenario was seen working in game, by name also `lab-brewing#placing-is-refused`, `lab-brewing#crafter-makes-filters`, `lab-brewing#lab-item-in-a-normal-recipe`, `lab-brewing#three-empty-slots`, `lab-brewing#matching-filter`, `lab-brewing#automatic-purification`, `lab-upgrades#water-cauldron-raises-the-yield`, `lab-cutting#automatic-cutting`, `lab-consumption#logging-out-does-not-skip-it`, `lab-consumption#hit-during-purity-5` and `lab-brewing#asking-for-the-tree`, or the failure is reported Also show `lab-brewing#watching-a-brew`, moved here from task 2.2.

## 11. Knowledge

```yaml
files: [knowledge/**]
depends: [10]
scenarios: []
agent: knowledge-curator
```

- [x] 11.1 Write the concepts listed in knowledge.md
  - Done when: `specfold validate` exits 0

## Workflow follow-up

- Archive the change, then run `specfold sync lab-brewing` and commit the result in the same PR.
