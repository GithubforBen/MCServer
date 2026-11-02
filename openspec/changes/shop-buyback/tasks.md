# Tasks

## 1. Offer data

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ItemForSale.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopkeeperStore.java
depends: []
scenarios:
  - shop-buyback#existing-offer-stays-sell-only
  - shop-buyback#new-offer-starts-without-buyback
  - shop-buyback#setting-survives-a-restart
  - shop-buyback#preset-from-the-sale-price
agent: implementer
```

- [x] 1.1 Add the buyback setting and price to offers, preset from the sale price when switched on
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [x] 1.2 Read and write the optional `buyback.*` keys, missing means off
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes; loading a real file needs a running server, so `shop-buyback#setting-survives-a-restart` and `shop-buyback#existing-offer-stays-sell-only` prove it in task 5.3

## 2. Selling

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/Shopkeeper.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/BuybackRules.java
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopUi.java
  - Survival/src/test/java/de/schnorrenbergers/survival/featrues/Shopkeeper/BuybackRulesCheck.java
depends: [1]
scenarios:
  - shop-buyback#selling-a-diamond-block
  - shop-buyback#member-of-the-owning-team-sells
  - shop-buyback#offer-without-buyback
  - shop-buyback#team-cannot-pay
  - shop-buyback#chest-is-full
  - shop-buyback#item-only-looks-the-same
  - shop-buyback#host-does-not-answer
  - shop-buyback#two-sellers-against-one-account
  - shop-buyback#offer-shows-its-buyback
agent: implementer
```

- [ ] 2.1 Write `BuybackRules` and `BuybackRulesCheck` for every refusal
  - Done when: `BuybackRulesCheck` runs and exits 0
- [ ] 2.2 Add the sale to `Shopkeeper`: reserve the lot, debit through the launcher, deliver, roll back on any failure
  - Done when: the sale compiles and `BuybackRulesCheck` still exits 0
- [ ] 2.3 Show the buyback in the shop menu and sell on right click
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes

## 3. Editor

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/ShopEditorUi.java
depends: [1]
scenarios:
  - shop-buyback#team-lowers-the-price
agent: implementer
```

- [ ] 3.1 Add the buyback switch and a price stepper to the offer menu
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes

## 4. Marketplace

```yaml
files:
  - Survival/src/main/java/de/schnorrenbergers/survival/featrues/Shopkeeper/market/**
depends: [2]
scenarios:
  - shop-buyback#listing-shows-the-best-buyer
  - shop-buyback#selling-from-the-offer-list
  - shop-buyback#sold-out-offer-still-buys
agent: implementer
```

- [ ] 4.1 Show the best buyback on listings and each offer's price in the offer list
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes
- [ ] 4.2 Sell on right click in the offer list
  - Done when: `./mvnw -B -q -pl Survival -am compile` passes

## 5. Docs and in-game check

```yaml
files:
  - README.md
  - testing/README.md
  - testing/shop-buyback/**
depends: [2, 3, 4]
scenarios: []
agent: implementer
```

- [ ] 5.1 Document buyback in the README and add a recipe to `testing/README.md`
  - Done when: both files describe the feature
- [ ] 5.2 Add `testing/shop-buyback/recipe.sh` naming, with a `spec: shop-buyback#<scenario>` comment, every scenario no check file covers
  - Done when: `specfold trace --change shop-buyback` reports 0 uncovered
- [ ] 5.3 Run the recipe on a local network with real clients
  - Done when: every scenario of `shop-buyback` was seen working in game, or the failure is reported

## 6. Knowledge

```yaml
files: [knowledge/**]
depends: [5]
scenarios: []
agent: knowledge-curator
```

- [ ] 6.1 Write the concepts listed in knowledge.md
  - Done when: `specfold validate` exits 0

## Workflow follow-up

- Archive the change, then run `specfold sync shop-buyback` and commit the result in the same PR.
