# shop-buyback Specification

## Purpose
Lets a shop buy goods back from players at a price its owning team sets, paid from the team account, so goods can flow to a shop without creating new bits.

## Requirements

### Requirement: Buyback is an opt-in setting of an offer

Every offer SHALL have a buyback setting that is off by default. An offer stored before this feature existed SHALL load with buyback off. Only members of the owning team SHALL be able to change the setting.

#### Scenario: Existing offer stays sell-only

- **WHEN** a shop stored before this feature is loaded
- **THEN** its offers sell as before and none of them buys anything

#### Scenario: New offer starts without buyback

- **WHEN** a team creates a new offer
- **THEN** buyback for that offer is off

#### Scenario: Setting survives a restart

- **WHEN** a team switches buyback on, sets a price and the server restarts
- **THEN** the offer still buys at that price

### Requirement: Buyback has its own price

An offer with buyback SHALL have a buyback price between 0 and 1,000,000 bits, independent of its sale price. When buyback is first switched on, the price SHALL be preset to the current sale price. Later changes to the sale price SHALL NOT change the buyback price.

#### Scenario: Preset from the sale price

- **WHEN** a team switches on buyback for an offer selling for 900 bits
- **THEN** the buyback price is 900 bits

#### Scenario: Team lowers the price

- **WHEN** the team sets the buyback price of that offer to 700 bits
- **THEN** the shop pays 700 bits and still sells for 900 bits

### Requirement: Players sell one lot with a right click

A right click on an offer with buyback in the shop menu SHALL sell one lot to the shop: the player gives the offer's amount of items identical to the offer's item, and receives the buyback price in bits. The goods SHALL land in the stock chest and the bits SHALL come out of the owning team's account. A left click SHALL keep buying. Anyone, including members of the owning team, MAY sell.

#### Scenario: Selling a diamond block

- **WHEN** a player holding a diamond block right-clicks an offer of one diamond block with a buyback price of 900 bits
- **THEN** the block moves from the player to the stock chest, the team account loses 900 bits and the player gains 900 bits

#### Scenario: Member of the owning team sells

- **WHEN** a member of the owning team sells a lot to the team's own shop
- **THEN** the sale goes through like any other

#### Scenario: Offer without buyback

- **WHEN** a player right-clicks an offer with buyback off
- **THEN** nothing is sold

### Requirement: A sale is all or nothing

The shop SHALL refuse a sale with a clear message, leaving goods and bits unchanged, when the player does not hold a full lot of identical items, the team account cannot cover the price, the stock chest cannot hold the lot, the chest is gone, the team no longer exists, or the host does not confirm the payment. A sale SHALL NOT create or destroy bits or items.

#### Scenario: Team cannot pay

- **WHEN** a player sells to a shop whose team account holds less than the buyback price
- **THEN** the sale is refused, the player keeps the goods and no bits move

#### Scenario: Chest is full

- **WHEN** the stock chest has no room for the lot
- **THEN** the sale is refused, the player keeps the goods and no bits move

#### Scenario: Item only looks the same

- **WHEN** a player offers a renamed or enchanted item against an offer for the plain item
- **THEN** the sale is refused

#### Scenario: Host does not answer

- **WHEN** the payment cannot be confirmed by the host
- **THEN** the player gets the goods back and no bits move

#### Scenario: Host answers too late

- **WHEN** the host only gets to a debit after the seller's wait has ended
- **THEN** the debit is dropped, the player has the goods back and no bits move

#### Scenario: Two sellers against one account

- **WHEN** two players sell at the same moment and the team account covers only one sale
- **THEN** exactly one sale goes through and the other player keeps the goods

### Requirement: Buyback is visible to customers

The shop menu SHALL show the buyback price and how to sell on every offer with buyback, and SHALL show nothing new on an offer without it.

#### Scenario: Offer shows its buyback

- **WHEN** a customer opens a shop with an offer that buys at 700 bits
- **THEN** that offer shows the price of 700 bits and that a right click sells

### Requirement: The marketplace shows and handles buyback

The marketplace SHALL show the highest price paid per item on a listing with buyback, and its offer list SHALL show each offer's buyback price. A right click on an offer in the offer list SHALL sell one lot, under the same rules as in the shop. A left click SHALL keep buying.

#### Scenario: Listing shows the best buyer

- **WHEN** two shops buy the same item at 700 and 800 bits per item
- **THEN** the listing shows 800 bits per item as the best buyback

#### Scenario: Selling from the offer list

- **WHEN** a player right-clicks an offer with buyback in the marketplace offer list
- **THEN** one lot is sold to that shop under the same rules as in the shop

#### Scenario: Sold-out offer still buys

- **WHEN** an offer has no stock but buyback is on
- **THEN** the offer list shows it as sold out and it still accepts sales
