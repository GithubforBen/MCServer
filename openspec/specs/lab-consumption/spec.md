# lab-consumption Specification

## Purpose
Defines what happens when a player consumes Kokain: effects by purity, cut-in effects, the crash afterwards and the limit in combat, so consuming is fun without deciding fights.

## Requirements

### Requirement: Consuming Kokain gives effects by purity

A right click with Kokain SHALL use up one item and give the effects of its purity, plus every effect cut into it at level I for the same duration.

| Purity | Speed | Haste | Duration |
| --- | --- | --- | --- |
| 1 | I | none | 30 s |
| 2 | I | I | 45 s |
| 3 | II | I | 60 s |
| 4 | II | II | 90 s |
| 5 | III | III | 120 s |

#### Scenario: Purity 3

- **WHEN** a player consumes Kokain of purity 3
- **THEN** they get Speed II and Haste I for 60 seconds and one item is gone

#### Scenario: Cut-in effect

- **WHEN** a player consumes Kokain of purity 1 with night vision
- **THEN** they also get night vision I for 30 seconds

### Requirement: A crash follows

When the effects end, the player SHALL get a crash of nausea and slowness that grows with purity. Logging out SHALL NOT skip the crash, and it SHALL happen on the next join if it fell due meanwhile. Dying SHALL cancel it.

| Purity | Nausea | Slowness | Duration |
| --- | --- | --- | --- |
| 1 | yes | none | 5 s |
| 2 | yes | none | 8 s |
| 3 | yes | I | 10 s |
| 4 | yes | I | 15 s |
| 5 | yes | II | 20 s |

#### Scenario: Crash after the effects

- **WHEN** the effects of Kokain of purity 5 end
- **THEN** the player gets nausea and Slowness II for 20 seconds

#### Scenario: Logging out does not skip it

- **WHEN** a player logs out with effects running and returns after they ended
- **THEN** the crash starts on join

#### Scenario: Death cancels it

- **WHEN** a player dies before the crash
- **THEN** no crash follows after respawning

### Requirement: Consuming has a cooldown

After consuming, a player SHALL NOT be able to consume Kokain again for 60 seconds, and the item SHALL stay in the inventory with a message.

#### Scenario: Too soon

- **WHEN** a player consumes again after 30 seconds
- **THEN** nothing is consumed and they are told how long is left

### Requirement: Above-vanilla effects stop in combat

A player is in combat for 15 seconds after dealing a hit to or taking a hit from another player. In combat, Speed and Haste from Kokain SHALL be at most level II. Effects above that SHALL drop to level II when combat starts and stay there until they run out, and Kokain consumed in combat SHALL give at most level II.

#### Scenario: Hit during purity 5

- **WHEN** a player with Speed III from Kokain is hit by another player
- **THEN** their Speed drops to II for the rest of its duration

#### Scenario: Consuming in combat

- **WHEN** a player consumes Kokain of purity 5 five seconds after a hit from another player
- **THEN** they get Speed II and Haste II

#### Scenario: Mob hit does not count

- **WHEN** a player with Speed III is hit by a zombie
- **THEN** they keep Speed III

### Requirement: Consumption is limited to Survival

Kokain SHALL only work on Survival. Consuming SHALL only be possible by choice, and no other player SHALL be affected.

#### Scenario: Holding it does nothing

- **WHEN** a player carries Kokain without using it
- **THEN** it has no effect on them or anyone near them
