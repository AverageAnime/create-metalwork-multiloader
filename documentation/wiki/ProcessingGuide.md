### ***3.1.0***

---

### Processing

| Mod                           | Routes                                                              |
|-------------------------------|---------------------------------------------------------------------|
| Create                        | Melting, casting, pressing, crushing to dust, milling, fan washing  |
| Create: Metallurgy            | Melting, bulk melting, alloying, casting in basin, casting in table |
| Create: Big Cannons           | Melting                                                             |
| Create: The Factory Must Grow | Industrial blasting, vat smelting + alloying, arc furnace, electrolysis, acid leaching, slag recovery, casting |
| Create: Simple Ore Doubling   | Ore doubling, for every supported metal                             |

Everything below is per **crushed ore**, and Create's wheels give exactly one of those per raw ore,
with a 75% chance of an experience nugget. The columns are how that crushed item was obtained; the
second and third need Create: Simple Ore Doubling, and give up that experience nugget for the extra
crushed ores — they pay only a 5% (lava) or 15% (heated) chance of a slag per raw ore instead.

Create's times are recipe durations that shorten as the machine spins faster, while TFMG's and Metallurgy's times are fixed.

| Route                                                           | Crushing wheels    | Compacting, lava | Compacting, heated |
|-----------------------------------------------------------------|--------------------|------------------|--------------------|
| Milled to dirty dust, 150 ticks, acid-leached in a vat          | 3.5x               | 7x               | 10.5x              |
| Milled to dirty dust, 150 ticks, arc furnace or electrolysis    | 3x                 | 6x               | 9x                 |
| Milled to dirty dust, 150 ticks, fireproof vat                  | 3x                 | 6x               | 9x                 |
| Milled to dirty dust, 150 ticks, steel vat                      | 2.75x              | 5.5x             | 8.25x              |
| Milled to dirty dust, 150 ticks, cast iron vat                  | 2.5x               | 5x               | 7.5x               |
| Crushed to dust in the wheels, 400 ticks, melted                | 2.5x               | 5x               | 7.5x               |
| Crushed to dust in the wheels, 400 ticks, vat-smelted           | 2.5x               | 5x               | 7.5x               |
| Melted in a heated mixer                                        | 2x                 | 4x               | 6x                 |
| Industrial blasting, 20 ticks                                   | 2x                 | 4x               | 6x                 |
| Melted in Metallurgy's melter, 48 ticks                         | 2x                 | 4x               | 6x                 |
| Milled to dust, 150 ticks, washed, melted                       | 1.25x              | 2.5x             | 3.75x              |
| Milled to dust, 150 ticks, washed, vat-smelted                  | 1.25x              | 2.5x             | 3.75x              |
| Pressed in a heated basin                                       | 1x                 | 2x               | 3x                 |
| Washed in a fan                                                 | 1x                 | 2x               | 3x                 |

* **The wheels** are the yield route on Create alone. They skip the dirty stage and the washing fan
  entirely, so they give up the byproducts below.
* **The millstone** pays half as much metal when its dirty dust is washed, and it is the only dust
  route that yields byproducts. It is faster on a single item — 1.25 dust in 150 ticks against 2.5 in
  400 — but it mills one item at a time, while the wheels take a whole stack at once and slow only
  with the log of its size, so fed in bulk the wheels are faster. With TFMG its dirty dust can
  instead go straight into a vat unwashed: the cast iron vat ties the wheels, and every TFMG stage
  past it pays more than they do.
* **Industrial blasting** matches the mixer in a fixed 20 ticks, for flux and hot air, and gives back
  slag and furnace gas. It takes raw ores and dusts too, at the same rate as the mixer.
* **The vat** matches melting a dust and hands 90% of its coke back. It runs one recipe at
  a time, and its speed does not depend on the mixer's RPM, so a better vat is how it gets faster.
* **The arc furnace** — three graphite electrodes on a fireproof vat — ties the fireproof vat on a dirty
  dust, 3x, in 40 ticks, with no heat, for flux, coke and power.
* **Electrolysis** — two electrodes on a steel or fireproof vat — ties the arc on aluminum and lithium
  dirty dust with no flux or coke, on a vat as small as 2 × 2 × 1, for heat and power.
* **Acid leaching** — a steel or fireproof vat with nothing attached — pays the most of any route, 3.5x,
  in 300 ticks, for 250 mB of TFMG sulfuric acid per dirty dust.
* **Metallurgy's melter** also matches the mixer, at 48 ticks, and returns 45 mB of its own molten
  slag with it. It melts every other form too, down to a nugget in 4 ticks. The eleven metals it
  ships a crushed ore for are restated at the same rate, so every metal pays alike there.
* **The press** is the only route needing no fluid at all, and **fan washing** no fluid and no heat.
* **Experience** comes from two places: crushing a raw ore in the wheels, and washing or crushing the
  slag a route leaves behind. Ore doubling gives up the first, and the routes that leave no slag —
  the wheels' dust, the mixer, the press, the fan, electrolysis and the leach — give up the second.
  The centrifuge turns slag into nuggets but no experience.

#### TFMG vats

Each TFMG vat runs the same recipes; a better vat is smaller and faster, and pays more for a dirty
dust. A clean dust pays 90 mB in every vat.

| Vat        | Minimum size, smelting / alloying | Clean dust | Dirty dust          | Alloying  |
|------------|-----------------------------------|------------|---------------------|-----------|
| Cast iron  | 12 / 18                           | 100 ticks  | 180 mB in 200 ticks | 150 ticks |
| Steel      | 9 / 12                            | 60 ticks   | 198 mB in 120 ticks | 100 ticks |
| Fireproof  | 4 / 9                             | 40 ticks   | 216 mB in 80 ticks  | 60 ticks  |

A vat's size is its width squared times its height, up to 3 × 3 × 3. A heated recipe needs two lit
blaze burners under the vat, or one superheated.

> Note: a vat reads its output tanks and slots as well as its inputs, so what one recipe leaves behind
> can start another in the same vat.

#### Steel

Steel is always 90 mB of molten iron and a coal or charcoal for 180 mB of molten steel, in a heated
mixer, a heated TFMG vat with a mixer, or TFMG's arc furnace. With Northstar, a heated mixer also
takes 180 mB of molten iron and a volcanic ash for 180 mB. With TFMG installed its blast furnace
makes molten iron like any other ore, not steel, so iron works the same with or without it.

> Note: one basin per step. A heated basin holding both a crushed ore and 90 mB of the metal matches
> the press and the cast at once, and Create picks one.

---

### Byproducts

Experience counts the slag washed in a fan, 0.35 of a nugget per 90 mB; crushing it as blocks pays a
little more, 3.5 per nine. Experience shown below is in addition to the 0.75 experience the wheels gave for the crushed ore itself.

| Route                               | Per crushed ore                                         | Experience |
|-------------------------------------|---------------------------------------------------------|------------|
| Crushed to dust in the wheels       | none                                                    | none       |
| Melted in a heated mixer            | none                                                    | none       |
| Pressed in a heated basin           | none                                                    | none       |
| Industrial blasting                 | 90 mB TFMG molten slag, 200 mB furnace gas              | 0.35       |
| Melted in Metallurgy's melter       | 45 mB Metallurgy molten slag                            | 0.18       |
| Milled to dust, washed              | 0.625 washed rolls                                      | none       |
| Washed, then vat-smelted            | 90% of the coke returned                                | none       |
| Dirty dust, vat-smelted             | 225 mB TFMG molten slag, 90% of the coke returned       | 0.88       |
| Dirty dust, arc furnace             | 225 mB TFMG molten slag, 90% of the coke returned       | 0.88       |
| Dirty dust, Metallurgy's melter     | 37.5 mB Metallurgy molten slag                          | 0.15       |
| Dirty dust, electrolysis or leached | none                                                    | none       |
| Washed in a fan                     | 9 nuggets and one byproduct roll                        | none       |

| Byproduct         | Metals                                             |
|-------------------|----------------------------------------------------|
| Redstone          | iron, tin                                          |
| Quartz            | gold, cincinnasite, midas gold, palladium, stormyx |
| Flint             | carmot, mythril, prometheum, quadrillum, runite    |
| Amethyst shard    | kyber, starrite                                    |
| Prismarine shard  | aquarium                                           |
| Ender dust        | thallasium                                         |
| Clay              | copper, andesite                                   |
| Gunpowder         | zinc, lithium, banglum                             |
| Iron nugget       | aluminum, manganese                                |
| Two iron nuggets  | titanium                                           |
| Copper nugget     | nickel, orichalcum                                 |
| Lead nugget       | silver, uranium                                    |
| Osmium nugget     | platinum, iridium                                  |
| Iridium nugget    | osmium                                             |
| Two zinc nuggets  | lead                                               |
| Gold nugget       | tungsten                                           |
| Two gold nuggets  | netherite scrap                                    |
| Powdered obsidian | adamantite                                         |

A lead, osmium or iridium nugget comes from whichever mod supplies that metal, and is clay where
none is installed. Quartz is kept for the ores that spawn in the Nether; the rest follow where they
spawn, or what the metal is mined with.

The crushing step follows Create's rule. A raw ore gives 1 crushed and a 75% experience nugget in 400
ticks; netherite scrap counts as one, being what each ancient debris gives. A silk-touched ore block
gives 2 crushed with a 25% chance of a third, a 75% experience nugget and a 12.5% chance of its host
rock back in 350 ticks, as Create's deepslate, Nether and gem ores do; the extra crushed makes up for
the Fortune the block could not get. That covers ancient debris, cincinnasite ore, and morkite and
starrite ore, which have no raw form. Andesite is its own: 9 crushed with a 10% chance of a tenth and a
55% experience nugget, since 9 crushed andesite melt to exactly one block.

#### Slag

Three supported mods have their own slag, and this makes every form of it interchangeable: TFMG's,
Metallurgy's and Simple Ore Doubling's slag items share `c:slag`, their blocks share
`c:storage_blocks/slag`, and TFMG's and Metallurgy's molten slag share `c:molten_slag`. Every recipe
now use the tags at one standard: **a slag is 90 mB, a block is nine slag and 810 mB**.

Slag only comes off raw ores, crushed ores and dirty dusts; a clean dust or an ingot does not.

| Slag                       | Made by                                                                    |
|----------------------------|----------------------------------------------------------------------------|
| TFMG molten slag           | Industrial blasting, vat smelting and the arc furnace, from ore forms      |
| Metallurgy molten slag     | Metallurgy's melter, from raw ores, crushed ores and dirty dusts           |
| Simple Ore Doubling slag   | Ore doubling, a 5% chance per raw ore with lava and 15% heated             |

* **Molten slag**:
  * Metallurgy's casting table makes a slag from 90 mB, and its casting basin a block from 810 mB.
  * TFMG's casting makes a block from 810 mB.
  * With Simple Ore Doubling, a press compacts 90 mB in a basin into one of its slag.
  * A TFMG vat with a centrifuge spins 90 mB into a 7.5% chance each of an iron, copper, gold and zinc
    nugget — washing a slag without casting it, less the experience.
* **Slag and blocks**:
  * Nine slag craft into a block and a block back into nine slag. Metallurgy's block and slag when it is
    installed, Simple Ore Doubling's otherwise.
  * Washed in a fan, a slag gives a 7.5% chance each of a copper, gold, iron and zinc nugget and a 35%
    experience nugget.
  * Crushed, a block gives three or four experience nuggets and a 75% chance each of the same four
    nuggets with Simple Ore Doubling, or nine `tfmg:slag` without it.
  * A slag mixes with eight each of bitumen or cement and gravel into 256 asphalt or concrete mixture.
  * Four slag compact with gravel and cobblestone into tuff.
  * A slag stonecuts into 18 TFMG slag bricks. A block does not; unpack it into nine slag first.

---

### Solidification

Molten metal poured into water solidifies into a metal block. Any `molten_<metal>` fluid in the game is picked up, and its target is whichever `c:storage_blocks/<metal>` block is actually installed. Resolved once, when block tags first load, so changes to `c:storage_blocks/*` mid-session requires a restart.

* Where several mods supply the same metal, the order is: Create, AllTheOres, Create Metallurgy, Create: Ironworks, Create Big Cannons, Northstar, Mythic Metals, TFMG, then anything else.

>Note: A source block is 1000 mB and the storage block it leaves behind is worth 810 mB, so pour into water for the convenience, not for the yield.
