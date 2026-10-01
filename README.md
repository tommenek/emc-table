# EMCompacted (Fabric, Minecraft 26.2)

An EMC system inspired by ProjectE / Equivalent Exchange, without the rest of it. Every item
gets an EMC value; a Transmutation Table lets you feed items in to bank their EMC and learn
them, then pull back out anything you have learnt.

Java 25, Fabric Loader 0.19.3+, Fabric API for 26.2.

## How values are decided
- **Raw materials** (ores, crops, mob drops, logs, stone) have hand-written values in
  `EmcValues.baseValues()`. Nothing can work these out for you.
- **Everything else** is derived from crafting recipes at server start: an item is worth what
  its ingredients are worth, divided by how many the recipe makes. This runs in several passes,
  because an ingredient may itself need deriving first.
- Each item keeps the **cheapest** value found, so an expensive recipe cannot inflate something
  you can make cheaply.
- Items with no value (no recipe and no base value) are simply not accepted by the table.
- Values recompute on `/reload`, so datapack recipe changes are picked up.

## Use
Right-click the table to open it.
- **Input slot (left):** put items in and they are consumed instantly - their EMC is added to
  your balance, and the item is added to your known list. Shift-clicking from your inventory
  sends items here.
- **Search box (top):** type to filter the known list by item name or id.
- **Known list (right):** everything you have learnt, with its icon and EMC cost, cheapest first.
  Rows you cannot currently afford are tinted red. Hover a row for the item's tooltip.
  - Left-click: withdraw one
  - Shift-click: withdraw a stack (or as many as you can afford)
- Your balance is shown in the top right.

EMC and knowledge are **per player**, saved with the world.

## EMC tooltips
Every item with a value shows `EMC: <value>` in its tooltip (and `Stack EMC` for stacks).
Press **H** (rebindable under Controls > EMC Table) to turn this on or off - it works in-game
and inside inventories. The choice is saved in `config/emcompacted.properties`.

## Crafting
```
Obsidian   Diamond        Obsidian
Diamond    Eye of Ender   Diamond
Obsidian   Diamond        Obsidian
```

## Balance warning
This is a duplication engine by design - that is what EMC is. Feeding in anything renewable and
cheap, then withdrawing something expensive, is the whole point of the system, and it will
trivialise resource gathering. Values in `EmcValues.baseValues()` are yours to tune.

Known sharp edges to watch for, as in ProjectE: items that can be farmed in bulk but derive a
high value through some recipe chain can become infinite-EMC loops. If you find one, lower its
base value or give it an explicit value.

## EMC Orb
A dense store of value - worth about **1,024,000 EMC**, worked out from its recipe like
everything else. Feed it to a table for a big deposit, or use it to craft the tablet.
```
Diamond Block   Emerald Block   Diamond Block
Emerald Block   Nether Star     Emerald Block
Diamond Block   Emerald Block   Diamond Block
```

## Transmutation Tablet
A wireless transmutation table: right-click it anywhere to open the same EMC menu, with the
same balance and knowledge. Costs roughly **4.1 million EMC** worth of materials.
```
EMC Orb       Ender Chest           EMC Orb
Ender Chest   Transmutation Table   Ender Chest
EMC Orb       Ender Chest           EMC Orb
```
