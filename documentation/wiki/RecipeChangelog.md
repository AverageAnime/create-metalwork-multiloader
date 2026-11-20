### ***3.1.0***

---

## Crushing & Washing

* Crushed ore melting now gives 180mB, up from 90mB.
* New heated compacting recipes to press a crushed ore straight into an ingot.
* New crushing recipes to turn a crushed ore into 2 dusts (+50%), skipping the dirty dust & fan washing steps. Best yield route on Create alone at 2.5x, and stacks with Create: Simple Ore Doubling up to 7.5x.
  * The milling chain is unchanged. Slower in total but faster per tick, and still the only source of washed byproducts.
* New ore hammer & `mekanism:crushing` recipes to turn any ingot back into a dust, and new smelting/blasting recipes to turn any dust back into an ingot.
* New splashing recipes so fan washing works on our crushed ores, giving 9 nuggets & a byproduct as it already did for Create's.
* Create's crushed aluminum, lead, nickel, osmium, silver, tin & uranium now fan-wash with any supported mod's nuggets, not only Immersive Engineering's, Mekanism's, Thermal's & a few others, and give the same byproduct as their dirty dust.
  * Crushed platinum & TFMG's crushed lithium can now be fan-washed too.
* Dirty tungsten dust now washes into a gold nugget, as Create: Metallurgy's does, instead of two zinc nuggets.
  * Northstar's crushed tungsten washes into the same instead of quartz, and Create: Metallurgy's crushed tungsten can now be fan-washed.
* Washing byproducts now vary by metal instead of most giving quartz or clay, each from whichever mod supplies it and clay where none does. Create's compat crushed ores wash into the same.
  * `silver`, `uranium` → lead nugget
  * `platinum` → osmium nugget
  * `osmium` → iridium nugget
  * `nickel`, `orichalcum` → copper nugget
  * `aluminum`, `manganese` → iron nugget
  * `aquarium` → prismarine shards
  * `kyber`, `starrite` → amethyst shards
  * `thallasium` → BetterEnd's ender dust
  * `banglum` → gunpowder
  * `carmot`, `mythril`, `prometheum`, `quadrillum`, `runite` → flint
  * Quartz stays on the Nether ores.
* Crushing an ore block now matches Create's deepslate, Nether & gem ores: 2 crushed with a 25% chance of a third, a 12.5% chance of the host rock back, in 350 ticks.
  * This covers starrite, morkite & cincinnasite ore, previously 1 with a 75% chance of a second in 400 ticks.
* Ancient debris can now be crushed, as an ore block.
* Netherite scrap now crushes like any raw ore: 1 crushed & a 75% experience nugget in 400 ticks, instead of a 50% chance of a second & 1.5 experience in 700.

## Melting, Casting & Alloying

* All alloy recipes now output twice as much.
* Molten metals now cast into whichever mod's ingot is installed. Mekanism, TFMG, Mythic Metals, Northstar & Create: Big Cannons ingots are now fallbacks for uranium, platinum, tungsten, osmium, silver, tin, lead, nickel, aluminum, steel, bronze, cast iron & lithium, several of which previously melted with no way back.
* New casting recipes for terminite, thallasium & cincinnasite, which previously had no way back from their molten form without Create: Metallurgy.
* Molten constantan, invar, iridium, lumium & signalum from AllTheOres, and obdurium, necromium & void steel from Create: Metallurgy, now work in every supported machine: melting, casting, Create & TFMG vat alloying at twice the source ratio, and Create: Big Cannons melting. AllTheOres ships no recipes for these fluids itself.

## Create: The Factory Must Grow

* Vat recipes now run in all three chemical vats.
  * Steel & fireproof vats need a smaller vat & run faster.
  * Vat times are reduced across the board (cast iron vat smelting 300 → 100 ticks), since a vat runs one recipe at a time regardless of mixer speed.
* New vat recipes for dirty dusts, paying 180mB, 198mB & 216mB by vat. 2.5x to 3x per crushed ore through the millstone, without fan washing, tying the wheels in the cast iron vat & beating them past it.
* New arc furnace recipes for every dirty dust (216mB) & raw ore (90mB).
* New electrolysis recipes for aluminum & lithium dirty dust (216mB).
* New acid leaching recipes, dirty dust & 250mB sulfuric acid into 252mB. The highest yield route at 3.5x per crushed ore.
* New centrifuge recipe recovering iron, copper, gold & zinc nuggets from 90mB of molten slag, as washing a slag would.
* Industrial blasting now pays the doubled amount for a crushed ore, and takes raw ores & dusts for every metal at the mixer's rate.
* Steel now follows the same process as everywhere else: the blast furnace melts iron ore into molten iron, and molten iron & coal make steel in a mixer, a vat or the arc furnace.

## Create: Metallurgy

* New melting recipes for dirty dusts & crushed ores, so its melter covers the same forms for our metals as it does for its own.
* Raw & crushed melting now give molten slag.
* Melting times now match Metallurgy per form instead of a flat 60 ticks.
* Its own crushed ore melting recipes are restated to 180mB.

## Create: Simple Ore Doubling

* Recipes now cover every supported metal, including lithium & tungsten.
* New compacting recipe to turn 90mB of molten slag into one slag.

## Slag

* TFMG, Create: Metallurgy & Create: Simple Ore Doubling slag are now interchangeable in every form, sharing new `c:molten_slag`, `c:slag` & `c:storage_blocks/slag` tags.
* All three now agree on one rate: a slag is 90mB & a block is 9 slag or 810mB.
  * TFMG casts its slag block from 810mB instead of 20mB, & crushes it into 9 slag instead of 2.3.
  * TFMG's asphalt & concrete mixtures from slag now take 8 bitumen or cement & 8 gravel per slag for 256, to compensate.
  * A slag stonecuts into 18 slag bricks. A slag block no longer stonecuts into bricks; unpack it into 9 slag first.

---

### ***3.0.0***

---

* Initial release.
