# lab-upgrades Specification

## Purpose
Defines how vanilla blocks near a brewing stand improve its lab brews, so a lab can grow without new blocks or items.

## Requirements

### Requirement: Blocks near a stand upgrade it

The following blocks within two blocks of a stand in every direction SHALL act as upgrades on lab brews, each counting once however many are present. Water cauldron: goods chance times 1.1. Amethyst block: goods chance times 1.1. Lightning rod: brew time 25 percent shorter. Smoker: one fuel item lasts 30 brews instead of 20. A chance SHALL never exceed 100 percent, and the extra goods chance SHALL come out of waste and nothing evenly.

#### Scenario: Water cauldron raises the yield

- **WHEN** a stand with a water cauldron beside it brews sugar cane
- **THEN** the goods chance is 52.8 percent

#### Scenario: Two cauldrons count once

- **WHEN** two water cauldrons stand within reach
- **THEN** the goods chance is multiplied by 1.1 once

#### Scenario: Empty cauldron does nothing

- **WHEN** only an empty cauldron stands within reach
- **THEN** the chance stays unchanged

#### Scenario: Lightning rod shortens a brew

- **WHEN** a stand with a lightning rod within reach brews
- **THEN** a brew takes 15 seconds

#### Scenario: Fully upgraded

- **WHEN** a stand has all four upgrades
- **THEN** one Kokain of purity 5 costs about 6,500 sugar cane on average

### Requirement: Upgrades take effect without any setup

Upgrades SHALL be read for each brew when it starts, so placing or breaking a block SHALL take effect from the next brew. Upgrades SHALL NOT change normal brewing.

#### Scenario: Removing an upgrade

- **WHEN** an amethyst block is broken during a brew
- **THEN** the running brew keeps its chances and the next one has none of that upgrade

#### Scenario: Normal potion near an upgrade

- **WHEN** a vanilla potion brews next to a lightning rod
- **THEN** it takes the normal time
