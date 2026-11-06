## Purpose

Defines how Kokain is changed after production, by cutting in effects or stretching it for quantity, so finished goods leave room to experiment and trade.

## ADDED Requirements

### Requirement: Ingredients add effects

A brew with Kokain in the bottom slots and one of these on top SHALL add its effect to each Kokain: golden carrot, night vision; phantom membrane, jump boost; fire charge, fire resistance; honey bottle, regeneration; magma cream, strength. One ingredient SHALL be used per brew. Purity SHALL stay the same, and the brew SHALL always succeed.

#### Scenario: Adding an effect

- **WHEN** Kokain of purity 3 is brewed with a golden carrot on top
- **THEN** it stays purity 3 and carries night vision

### Requirement: At most three distinct effects

An item SHALL carry up to three effects, no effect twice, and the order SHALL not matter: items with the same purity, the same effects and the same stretched state SHALL stack.

#### Scenario: Fourth ingredient

- **WHEN** Kokain with three effects has a fourth ingredient on top
- **THEN** no brew starts for that item

#### Scenario: Same effect twice

- **WHEN** Kokain with night vision has a golden carrot on top
- **THEN** no brew starts for that item

#### Scenario: Order does not matter

- **WHEN** one Kokain got a golden carrot then magma cream, and another of equal purity magma cream then a golden carrot
- **THEN** both stack

### Requirement: Stretching makes three of lower purity

A brew with Kokain of purity 2 or higher in a bottom slot and sugar on top SHALL turn that Kokain into three of one purity lower, keeping its effects and marked as stretched. One sugar SHALL be used per brew. Stretched Kokain and Kokain of purity 1 SHALL NOT stretch.

#### Scenario: Stretching

- **WHEN** one Kokain of purity 4 is brewed with sugar on top
- **THEN** the slot holds three stretched Kokain of purity 3

#### Scenario: No second stretch

- **WHEN** stretched Kokain has sugar on top
- **THEN** no brew starts for that item

#### Scenario: Lowest purity

- **WHEN** Kokain of purity 1 has sugar on top
- **THEN** no brew starts for that item

### Requirement: Cutting works through hoppers

Cutting and stretching SHALL be automatable under the same hopper rules as the production chain.

#### Scenario: Automatic cutting

- **WHEN** a side hopper feeds Kokain and a top hopper feeds golden carrots
- **THEN** the stand cuts without a player and a hopper below takes the result
