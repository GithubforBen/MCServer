# lab-brewing Specification

## Purpose
Defines the production chain in the brewing stand: from sugar cane through five purity tiers of paste to Kokain, with waste, falling yields, automation and persistence, as an addon that leaves everything else in Survival unchanged.

## Requirements

### Requirement: Sugar cane brews into paste with empty output slots

A brewing stand with sugar cane on top, fuel and at least one empty bottom slot SHALL brew without any bottle. Each brew SHALL use one sugar cane per slot it rolls, and each rolled slot SHALL end with Rohpaste of purity 1 at 48 percent, Pflanzenabfall at 32 percent, or stay empty at 20 percent. With fewer sugar cane than empty slots, only that many slots SHALL roll.

#### Scenario: Three empty slots

- **WHEN** a brew finishes with three empty slots and at least three sugar cane on top
- **THEN** three sugar cane are used and each slot rolled once

#### Scenario: Two sugar cane left

- **WHEN** a brew runs with three empty slots and two sugar cane on top
- **THEN** two sugar cane are used and exactly two slots rolled

#### Scenario: No free slot

- **WHEN** every bottom slot holds something that is not an input of the top ingredient
- **THEN** no brew starts and no sugar cane is used

### Requirement: Paste is purified with the matching filter

A brew with a filter on top SHALL work on each bottom slot holding paste of the filter's tier, and one filter SHALL be used per brew, for up to three items. Each item SHALL roll on its own for the next purity, Pflanzenabfall or nothing, and "nothing" SHALL leave the slot empty. The chances fall with every tier as the scenarios state.

#### Scenario: Papierfilter chances

- **WHEN** purity 1 paste is brewed with a Papierfilter
- **THEN** it becomes purity 2 at 35 percent, waste at 40 percent, or nothing at 25 percent

#### Scenario: Kohlefilter chances

- **WHEN** purity 2 paste is brewed with a Kohlefilter
- **THEN** it becomes purity 3 at 15 percent, waste at 50 percent, or nothing at 35 percent

#### Scenario: Seidenfilter chances

- **WHEN** purity 3 paste is brewed with a Seidenfilter
- **THEN** it becomes purity 4 at 6 percent, waste at 54 percent, or nothing at 40 percent

#### Scenario: Goldfilter chances

- **WHEN** purity 4 paste is brewed with a Goldfilter
- **THEN** it becomes purity 5 at 4 percent, waste at 56 percent, or nothing at 40 percent

#### Scenario: Matching filter

- **WHEN** a brew finishes with a Papierfilter on top and purity 1 paste in all three slots
- **THEN** one filter is used and each of the three items became purity 2, waste or nothing

#### Scenario: Wrong filter

- **WHEN** a Goldfilter lies on top of purity 1 paste
- **THEN** no brew starts

#### Scenario: Mixed tiers

- **WHEN** a Papierfilter lies on top of two items of purity 1 and one of purity 2
- **THEN** the brew works on the two items of purity 1 and leaves the other untouched

### Requirement: Paste of any tier becomes Kokain of that tier

A brew with Feenstaub on top SHALL turn each bottom paste into Kokain of the same purity at 83 percent, Pflanzenabfall at 12 percent, or nothing at 5 percent. One Feenstaub SHALL be used per brew.

#### Scenario: Purity carries over

- **WHEN** purity 3 paste becomes Kokain
- **THEN** the Kokain has purity 3

### Requirement: The chain averages 20,000 sugar cane at the top

With default values and no upgrades, the average sugar cane used for one Kokain of purity 5 SHALL be between 19,000 and 21,000. Yields, the brew time of 20 seconds and fuel behavior SHALL be configurable without code changes, and missing values SHALL be written with their defaults.

#### Scenario: Simulated average

- **WHEN** the chain with default values is simulated a large number of times
- **THEN** the average sugar cane per Kokain of purity 5 lies between 19,000 and 21,000

### Requirement: Filters and Feenstaub are crafted

The following SHALL craft at a crafting table and in a crafter. Papierfilter: 3 paper make 8. Kohlefilter: Papierfilter and charcoal. Seidenfilter: Kohlefilter and 4 string. Goldfilter: Seidenfilter and 8 gold nuggets. Feenstaub: glowstone dust and sugar.

#### Scenario: Crafter makes filters

- **WHEN** a crafter holds 3 paper in the right pattern and is triggered
- **THEN** it puts out 8 Papierfilter

#### Scenario: Plain item does not pass as a filter

- **WHEN** a player tries to craft a Kohlefilter from plain paper and charcoal
- **THEN** no recipe matches

### Requirement: Pflanzenabfall is junk that blocks its slot

Pflanzenabfall SHALL look like a dead bush, carry its own name and be impossible to place as a block. It SHALL stay in its slot until removed, and a slot holding it SHALL not roll.

#### Scenario: Placing is refused

- **WHEN** a player tries to place Pflanzenabfall
- **THEN** no block appears and the item stays in the inventory

#### Scenario: Waste blocks the slot

- **WHEN** a slot holds Pflanzenabfall and a brew runs
- **THEN** that slot rolls nothing and keeps its waste

### Requirement: The chain works through hoppers

Hoppers SHALL be able to put sugar cane, filters, Feenstaub and cutting ingredients in from above, and fuel, paste and Kokain in from the side. A hopper below SHALL be able to take goods and waste out of the bottom slots, and SHALL NOT take an item out while the ingredient on top is a valid step for that item.

#### Scenario: Sugar cane through a hopper

- **WHEN** a hopper above a stand holds sugar cane
- **THEN** the sugar cane moves into the top slot

#### Scenario: Automatic purification

- **WHEN** a side hopper feeds purity 1 paste, a top hopper feeds Papierfilter and fuel is present
- **THEN** the stand brews without a player

#### Scenario: Input is not pulled out

- **WHEN** a hopper below a stand faces purity 1 paste while a Papierfilter is on top
- **THEN** the paste stays in the stand until it was brewed

#### Scenario: Results are pulled out

- **WHEN** a brew left Rohpaste and Pflanzenabfall in the slots
- **THEN** a hopper below takes both out

### Requirement: Brews survive unloading and restarts

A brew's progress SHALL pause while its chunk is unloaded and continue from the same point when it loads again, and SHALL survive a server restart, losing at most a few seconds. Ingredients SHALL be used only when a brew finishes. No sequence of unloading, restarting, or breaking SHALL duplicate or lose items beyond the running brew's fuel use.

#### Scenario: Chunk unloads mid-brew

- **WHEN** the chunk unloads at half progress and loads again
- **THEN** the brew continues from about half

#### Scenario: Restart mid-brew

- **WHEN** the server stops at half progress and starts again
- **THEN** the brew continues from about half and no second brew runs in parallel

#### Scenario: Crash mid-brew

- **WHEN** the server dies without shutting down during a brew
- **THEN** the brew continues after the start and no item was duplicated

#### Scenario: Stand broken mid-brew

- **WHEN** a player breaks a stand during a brew
- **THEN** its contents drop as usual, progress is gone and nothing was rolled

#### Scenario: Ingredient taken out mid-brew

- **WHEN** a player takes the top ingredient out during a brew
- **THEN** the brew stops and starts over when an ingredient returns

#### Scenario: Many stands reloading

- **WHEN** a chunk with many running stands unloads and loads repeatedly
- **THEN** each stand continues its own brew and none runs twice

### Requirement: A running brew is visible

A running brew SHALL show its progress in the stand's menu to everyone who has it open.

#### Scenario: Watching a brew

- **WHEN** a player opens a stand during a brew
- **THEN** the progress arrow matches the brew's progress

### Requirement: Nothing changes for people who do not take part

Normal brewing SHALL work unchanged, and lab items SHALL NOT work as their plain look-alikes in normal crafting or brewing. The system SHALL exist on Survival only, and SHALL show nothing to players who do not use it: no messages, no unlocked recipes popping up, no changed items.

#### Scenario: Normal potion

- **WHEN** a player brews a vanilla potion with water bottles
- **THEN** it brews exactly as in vanilla

#### Scenario: Lab item in a normal recipe

- **WHEN** a player uses a Papierfilter in a recipe that takes paper
- **THEN** the recipe does not match

#### Scenario: Bystander sees nothing

- **WHEN** a player who never used the lab plays Survival
- **THEN** they get no lab message and no recipe notification

### Requirement: Players can find out how it works

`/labor` SHALL show the production tree in chat, and every lab item SHALL name its purity if it has one and its next step.

#### Scenario: Asking for the tree

- **WHEN** a player runs `/labor`
- **THEN** the chat shows the chain from sugar cane to Kokain

#### Scenario: Item names the next step

- **WHEN** a player looks at purity 1 paste
- **THEN** its description names the Papierfilter as the next step
