# item-signing Specification

## Purpose
Lets a player put their name on items so that goods can be traced to who made them, with signed goods kept intact through crafting and traded in shops as distinct goods.

## Requirements

### Requirement: Players sign the stack in their hand

`/signItem` SHALL sign the whole stack in the player's main hand with that player, for free, and add a line naming the player to the item's description. An empty hand SHALL be answered with a message and nothing else.

#### Scenario: Signing a stack

- **WHEN** a player holding 32 cobblestone runs `/signItem`
- **THEN** all 32 carry that player's signature and their description names the player

#### Scenario: Empty hand

- **WHEN** a player with an empty main hand runs `/signItem`
- **THEN** they are told to hold an item and nothing changes

### Requirement: Same signer, same item

Items of the same kind signed by the same player under the same name SHALL be identical, so they stack, however often they were signed. The description SHALL show the name the player had when signing; after a rename, newly signed items show the new name and need not stack with older ones.

#### Scenario: Signed ten times

- **WHEN** a player signs ten single cobblestone one after another
- **THEN** the ten stack into one stack of ten

#### Scenario: Signing twice

- **WHEN** a player runs `/signItem` on items they already signed
- **THEN** the items stay exactly as they were and the player is told they are already signed

### Requirement: A signature is final

An item signed by one player SHALL NOT be signed by another, and items that carry a function of the network, such as gadgets, cosmetic items or menu items, SHALL NOT be signed.

#### Scenario: Someone else's signature

- **WHEN** a player runs `/signItem` on items signed by another player
- **THEN** the items keep the first signature and the player is told who signed them

#### Scenario: Gadget item

- **WHEN** a player runs `/signItem` holding a gadget item
- **THEN** nothing changes and the player is told it cannot be signed

### Requirement: Signed items are never consumed as an ingredient

A signed item SHALL NOT be consumed by crafting at a crafting table, by a crafter, by smelting in a furnace, blast furnace or smoker, by a smithing table, by a stonecutter, by a grindstone, by a lab brew or a vanilla potion brew, or as the sacrifice on the right of an anvil. Where an automatic block would pick a signed item, it SHALL not use it. As the left item of an anvil, the result SHALL keep the signature.

#### Scenario: Crafting table

- **WHEN** a player puts signed planks into a crafting table
- **THEN** no recipe result appears for them

#### Scenario: Crafter skips signed items

- **WHEN** a crafter holds signed planks and is triggered
- **THEN** it crafts nothing from them and the planks stay signed in the crafter

#### Scenario: Furnace

- **WHEN** signed iron ore goes into a furnace through a hopper
- **THEN** it is not smelted

#### Scenario: Anvil left and right

- **WHEN** a signed sword lies on the left of an anvil with a book on the right
- **THEN** the result keeps the signature, and a signed item on the right gives no result

#### Scenario: Stonecutter

- **WHEN** a player puts signed cobblestone into a stonecutter
- **THEN** no result can be taken and the cobblestone stays signed

#### Scenario: Grindstone

- **WHEN** a player puts a signed item into a grindstone
- **THEN** no result appears

#### Scenario: Potion brewing

- **WHEN** a brewing stand has signed nether wart on top and water bottles below
- **THEN** it does not brew and the nether wart stays signed

### Requirement: Shops trade signed goods as their own goods

A shop SHALL sell and buy back signed goods like any other item. Signed and unsigned items of the same kind, and items signed by different players, SHALL count as different goods for stock, sales and buyback.

#### Scenario: Selling signed goods

- **WHEN** a shop offers cobblestone signed by Ben and its chest holds such cobblestone
- **THEN** a customer can buy it and receives cobblestone signed by Ben

#### Scenario: Unsigned is not signed

- **WHEN** the chest of that shop only holds unsigned cobblestone
- **THEN** the offer shows no stock

#### Scenario: Buyback of signed goods

- **WHEN** that offer buys back and a player sells cobblestone signed by Ben
- **THEN** the sale goes through, and cobblestone signed by someone else is refused
