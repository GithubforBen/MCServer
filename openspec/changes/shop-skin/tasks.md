# Tasks

## 1. Shop figure

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/Shopkeeper.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopkeeperStore.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopkeeperListener.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopkeeperChunkListener.java
depends: []
scenarios:
  - shop-skin#existing-shop
  - shop-skin#customer-clicks-a-skinned-shop
  - shop-skin#owner-sneak-clicks-a-skinned-shop
  - shop-skin#skinned-shop-cannot-be-hurt
  - shop-skin#moving-a-skinned-shop
  - shop-skin#restart
  - shop-skin#chunk-reload
  - shop-skin#skin-removed-while-the-chunk-reloads-later
agent: implementer
```

- [x] 1.1 Store an optional skin on a shop, missing means none
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; loading a real file needs a running server, so `shop-skin#existing-shop` and `shop-skin#restart` prove it in task 4.1
- [x] 1.2 Let a shop stand as a villager or a mannequin, with one adopt path for both
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [ ] 1.3 Replace a figure of the wrong kind on adopt and on a skin change
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes

## 2. Editor

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopEditorUi.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/SkinName.java
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/Shopkeeper/SkinNameCheck.java
depends: [1]
scenarios:
  - shop-skin#setting-a-skin
  - shop-skin#invalid-name
  - shop-skin#removing-the-skin
  - shop-skin#cancelling
  - shop-skin#stranger-cannot-reach-it
agent: implementer
```

- [ ] 2.1 Write the name rule as pure code with `SkinNameCheck`
  - Done when: `SkinNameCheck` runs and exits 0
- [ ] 2.2 Add the skin button and the chat prompt to the settings menu
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes

## 3. Docs and recipe

```yaml
files:
  - README.md
  - testing/README.md
  - testing/shop-skin/**
depends: [2]
scenarios: []
agent: implementer
```

- [ ] 3.1 Document skins in the README and add a recipe to `testing/README.md`
  - Done when: both files describe the feature
- [ ] 3.2 Add `testing/shop-skin/recipe.sh` naming, with a `spec: shop-skin#<scenario>` comment, every scenario no check file covers
  - Done when: `specfold trace --change shop-skin` reports 0 uncovered

## 4. In-game check

```yaml
files:
  - testing/shop-skin/**
depends: [3]
scenarios: []
agent: implementer
```

- [ ] 4.1 Run the recipe on a local network with real clients, including a restart
  - Done when: every scenario of `shop-skin` was seen working in game, or the failure is reported

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

- Archive the change, then run `specfold sync shop-skin` and commit the result in the same PR.
