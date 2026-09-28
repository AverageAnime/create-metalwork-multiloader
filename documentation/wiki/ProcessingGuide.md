### ***3.0.0***

---

### Processing

| Mod                           | Routes                                                              |
|-------------------------------|---------------------------------------------------------------------|
| Create: Metallurgy            | Melting, bulk melting, alloying, casting in basin, casting in table |
| Create: Big Cannons           | Melting                                                             |
| Create: The Factory Must Grow | Industrial blasting, vat smelting + alloying, casting               |

| Route                               | Milling / Crushing | Compacting, lava | Compacting, heated |
|-------------------------------------|--------------------|------------------|--------------------|
| Industrial blasting, 20 ticks       | 1x                 | 2x               | 3x                 |
| Melted in a heated mixer, 100 ticks | 1x                 | 2x               | 3x                 |
| Milled to dust, washed, melted      | 1.25x              | 2.5x             | 3.75x              |
| Milled to dust, washed, vat-smelted | 2.5x               | 5x               | 7.5x               |

---

### Solidification

Molten metal poured into water solidifies into a metal block. Any `molten_<metal>` fluid in the game is picked up, and its target is whichever `c:storage_blocks/<metal>` block is actually installed. Resolved once, when block tags first load, so changes to `c:storage_blocks/*` mid-session requires a restart.

* Where several mods supply the same metal, the order is: Create, AllTheOres, Create Metallurgy, Create: Ironworks, Create Big Cannons, Northstar, Mythic Metals, TFMG, then anything else.

>Note: A source block is 1000 mB and the storage block it leaves behind is worth 810 mB, so pour into water for the convenience, not for the yield.
