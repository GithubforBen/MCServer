# Tasks

## 1. Prove empty slots on a real server

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

- [ ] 1.1 Write the brew state as a pure class with `LabBrewCheck`, covering pause, resume, ingredient loss and double start
  - Done when: `LabBrewCheck` runs and exits 0
- [ ] 1.2 Run the own tick on a stand with empty slots, persisted in the block
  - Done when: sugar cane brews into a placeholder result on a local server
- [ ] 1.3 Prove top-slot hoppers, the hopper-out rule and the progress arrow on a local server
  - Done when: `lab-brewing#sugar-cane-through-a-hopper` and `lab-brewing#watching-a-brew` were seen working
- [ ] 1.4 Run the unload matrix: chunk unload, repeated reloads with many stands, restart, `kill -9`, breaking, taking the ingredient
  - Done when: every case was seen working, or the failure is reported to the user with the glass bottle fallback as the question

## 2. Items and config

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/item/**
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/LabSettings.java
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/item/**
depends: [1]
scenarios:
  - lab-brewing#placing-is-refused
  - lab-brewing#item-names-the-next-step
agent: implementer
```

- [ ] 2.1 Build paste, Kokain, filters, Feenstaub and Pflanzenabfall without a resource pack
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [ ] 2.2 Read `configs/lab.yml` with defaults written back
  - Done when: a missing file is created with every default
- [ ] 2.3 Refuse placing Pflanzenabfall
  - Done when: `lab-brewing#placing-is-refused` was seen working

## 3. Crafting

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/craft/**
depends: [2]
scenarios:
  - lab-brewing#crafter-makes-filters
  - lab-brewing#plain-item-does-not-pass-as-a-filter
  - lab-brewing#lab-item-in-a-normal-recipe
  - lab-brewing#bystander-sees-nothing
agent: implementer
```

- [ ] 3.1 Register the filter and Feenstaub recipes, undiscovered so nobody gets a notification
  - Done when: `lab-brewing#crafter-makes-filters` was seen working
- [ ] 3.2 Keep lab items out of normal recipes
  - Done when: `lab-brewing#lab-item-in-a-normal-recipe` was seen working

## 4. Production chain

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/chain/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/chain/**
depends: [2]
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

- [ ] 4.1 Write the step table and rolling as pure code with `LabChainCheck`
  - Done when: `LabChainCheck` runs and exits 0
- [ ] 4.2 Prove the chain's average with a seeded simulation
  - Done when: `lab-brewing#simulated-average` passes in `LabChainCheck`
- [ ] 4.3 Connect sugar cane to the own tick
  - Done when: `lab-brewing#three-empty-slots` was seen working
- [ ] 4.4 Run purifying and Kokain through `BrewingRecipe` and `BrewEvent`
  - Done when: `lab-brewing#matching-filter` was seen working
- [ ] 4.5 Apply the hopper rule to all steps
  - Done when: `lab-brewing#automatic-purification` was seen working

## 5. Upgrades

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/upgrade/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/upgrade/**
depends: [4]
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

- [ ] 5.1 Compute chances from upgrades as pure code with `LabUpgradeCheck`, including the 6,400 average
  - Done when: `LabUpgradeCheck` runs and exits 0
- [ ] 5.2 Read the blocks around a stand when a brew starts
  - Done when: `lab-upgrades#water-cauldron-raises-the-yield` was seen working

## 6. Cutting

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/cutting/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/cutting/**
depends: [4]
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

- [ ] 6.1 Write cutting and stretching as pure code with `LabCuttingCheck`
  - Done when: `LabCuttingCheck` runs and exits 0
- [ ] 6.2 Run them through `BrewingRecipe` and `BrewEvent`, with stacking by sorted effects
  - Done when: `lab-cutting#automatic-cutting` was seen working

## 7. Consumption

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/consume/**
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/lab/consume/**
depends: [2]
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

- [ ] 7.1 Write effect tables and the combat cap as pure code with `LabConsumeCheck`
  - Done when: `LabConsumeCheck` runs and exits 0
- [ ] 7.2 Consume on right click, with cooldown and crash, surviving a relog
  - Done when: `lab-consumption#logging-out-does-not-skip-it` was seen working
- [ ] 7.3 Track combat between players and cap the effects
  - Done when: `lab-consumption#hit-during-purity-5` was seen working

## 8. Command, docs and full run

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/lab/LabCommand.java
  - Survival/src/main/resources/plugin.yml
  - README.md
  - testing/README.md
  - ServerLauncherApplication/src/main/resources/discord-info/spieler.md
depends: [3, 5, 6, 7]
scenarios:
  - lab-brewing#asking-for-the-tree
agent: implementer
```

- [ ] 8.1 Add `/labor` with the tree
  - Done when: `lab-brewing#asking-for-the-tree` was seen working
- [ ] 8.2 Document the lab in the README, the player info and `testing/README.md`
  - Done when: all three describe the feature
- [ ] 8.3 Run a whole automated line on a local network from sugar cane to consumed Kokain
  - Done when: the line ran without a player touching it, or the failure is reported

## 9. Knowledge

```yaml
files: [knowledge/**]
depends: [8]
scenarios: []
agent: knowledge-curator
```

- [ ] 9.1 Write the concepts listed in knowledge.md
  - Done when: `specfold validate` exits 0

## Workflow follow-up

- Archive the change, then run `specfold sync lab-brewing` and commit the result in the same PR.
