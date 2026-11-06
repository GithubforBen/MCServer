# shop-skin Specification

## Purpose
Lets a team give its shop the skin of a Minecraft account, so a shop can look like a person instead of a villager, without changing how the shop works.

## Requirements

### Requirement: The owning team sets a skin by player name

The shop editor SHALL offer members of the owning team a way to give the shop the skin of a Minecraft account by typing its name. A name outside 3 to 16 letters, digits and underscores SHALL be refused with a message. Typing `aus` SHALL remove the skin, typing `abbrechen` SHALL leave it unchanged. Only members of the owning team SHALL reach this.

#### Scenario: Setting a skin

- **WHEN** a team member enters the player name `Notch` in the shop editor
- **THEN** the shop stands as a player figure wearing that account's skin

#### Scenario: Invalid name

- **WHEN** a team member enters `not a name!`
- **THEN** the skin stays as it was and the member is told the name is invalid

#### Scenario: Removing the skin

- **WHEN** a team member enters `aus`
- **THEN** the shop stands as a villager again

#### Scenario: Cancelling

- **WHEN** a team member enters `abbrechen`
- **THEN** nothing changes

#### Scenario: Stranger cannot reach it

- **WHEN** a player who is not in the owning team sneak-clicks the shop
- **THEN** the shop editor does not open

### Requirement: Shops without a skin stay as they are

A shop without a skin SHALL remain a villager. A shop stored before this feature existed SHALL load without a skin.

#### Scenario: Existing shop

- **WHEN** a shop stored before this feature is loaded
- **THEN** it stands as a villager as before

### Requirement: A shop with a skin behaves like any shop

A shop with a skin SHALL open the same menus on the same clicks, show its name, stand still, be impossible to kill, and be movable with the existing move function. There SHALL be exactly one figure per shop at all times.

#### Scenario: Customer clicks a skinned shop

- **WHEN** a customer right-clicks a shop with a skin
- **THEN** the shop menu opens

#### Scenario: Owner sneak-clicks a skinned shop

- **WHEN** a team member sneak-right-clicks a shop with a skin
- **THEN** the shop editor opens

#### Scenario: Skinned shop cannot be hurt

- **WHEN** a player attacks a shop with a skin
- **THEN** it takes no damage

#### Scenario: Moving a skinned shop

- **WHEN** a team moves a shop with a skin
- **THEN** one figure stands at the new spot and none at the old one

### Requirement: The skin survives restarts and chunk reloads

A shop's skin SHALL persist, and restarting the server, reloading the chunk or changing the skin SHALL leave exactly one figure of the right kind.

#### Scenario: Restart

- **WHEN** the server restarts with a skinned shop in the world
- **THEN** exactly one figure with that skin stands there and no villager

#### Scenario: Chunk reload

- **WHEN** the chunk of a skinned shop unloads and loads again
- **THEN** exactly one figure with that skin stands there

#### Scenario: Skin removed while the chunk reloads later

- **WHEN** a skin is removed and the chunk is reloaded afterwards
- **THEN** exactly one villager stands there
