# EMC Table (Fabric, Minecraft 26.2)

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
- **Known list (right):** everything you have learnt, cheapest first, with its EMC cost.
  Rows you cannot currently afford are tinted red.
  - Left-click: withdraw one
  - Shift-click: withdraw a stack (or as many as you can afford)
- Your balance is shown in the top right.

EMC and knowledge are **per player**, saved with the world.

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
