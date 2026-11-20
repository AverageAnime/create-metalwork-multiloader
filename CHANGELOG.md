### *** 3.1.0 ***

#### Changes:
* Additional logging for clarity. [(#2)](https://github.com/AverageAnime/create-metalwork-multiloader/issues/2)
  * Now reports skipped config entries and why.
  * Entries without an `@` condition now warn that the default condition was applied. Use `@always` to register it unconditionally.
* Crushed ore melting now gives 180mB, up from 90mB, restoring the yield bonus from prior versions. [(#3)](https://github.com/AverageAnime/create-metalwork-multiloader/issues/3)
* Various recipe chains have been updated. See the full list of changes [here](https://github.com/AverageAnime/create-metalwork-multiloader/wiki/Recipe-Changelog) and read the updated processing guide [here](https://github.com/AverageAnime/create-metalwork-multiloader/wiki/Processing-Guide).
  * All alloy recipes now output twice as much. [(#4)](https://github.com/AverageAnime/create-metalwork-multiloader/issues/4)
  * Create: Simple Ore Doubling recipes now cover every supported metal.
  * Crushed ores can now be pressed straight into an ingot with heated compacting, or crushed into 2 dusts, skipping the dirty dust & fan washing steps.
  * Fan washing now works on our crushed ores, and on more of Create's.
  * Washing byproducts now vary by metal instead of most giving quartz or clay.
  * Crushing an ore block now matches Create's deepslate, Nether & gem ores. Ancient debris can now be crushed.
  * TFMG, Create: Metallurgy & Create: Simple Ore Doubling slag are now interchangeable, sharing new tags.
  * Expanded Create: The Factory Must Grow compatibility.
    * Vat recipes now run in all three chemical vats, and run faster.
    * New vat, arc furnace, electrolysis & acid leaching recipes for dirty dusts.
    * New centrifuge recipe recovering nuggets from molten slag.
    * Steel now follows the same process as everywhere else: molten iron & coal in a mixer, a vat or the arc furnace.
  * Expanded Create: Metallurgy compatibility.
    * New melting recipes for dirty dusts & crushed ores. Raw & crushed melting now give molten slag.
    * Its own crushed ore melting recipes are restated to 180mB.
* New crushed raw titanium, titanium dust & dirty titanium dust with Northstar, and new crushed raw iridium with AllTheOres.
* New molten constantan, for TFMG's constantan when neither AllTheOres nor Create: Metallurgy is installed.
* Molten metals now cast into whichever mod's ingot is installed, so several that previously melted with no way back can now be cast.
* Molten constantan, invar, iridium, lumium & signalum from AllTheOres, and obdurium, necromium & void steel from Create: Metallurgy, now work in every supported machine.
* Compatibility for Mekanism dusts. Its own dirty dusts & dusts are now used for the seven metals it covers, instead of registering ours.
* Removed `crushed_raw_tungsten`, which nothing could produce. Create: Metallurgy's & Northstar's are used instead. **Existing crushed raw tungsten will be removed** once its line is deleted from `createmetalwork-common.toml`.
* Mythril no longer checks for Create: Dreams & Desires.

#### Fixes:
* Fix crushed ores for Mythic Metals, BetterEnd & BetterNether metals having no recipe, leaving their melting recipes unreachable. [(#5)](https://github.com/AverageAnime/create-metalwork-multiloader/issues/5)
* Fix TFMG vat smelting duplication loop with an ore hammer. Clean dusts no longer give molten slag.
* Fix duplicate milling & splashing recipes with Create: Metallurgy.
* Fix Create: Metallurgy melting recipes having no heat requirement.
* Fix `c:raw_materials` not being bridged for Mythic Metals, BetterEnd & BetterNether. Cincinnasite shards now crush & melt like any raw ore.
* Fix missing `c:crushed_raw_materials/morkite` tag.
* Fix aeternium casting to a non-existent item instead of BetterEnd's.
* Fix crushed tin from Create: Ironworks, crushed lithium from TFMG & crushed tungsten from Create: Metallurgy not being recognized as crushed ores.
* Fix Mythic Metals raw osmium, platinum, silver & tin not being crushable.

### *** 3.0.0 ***

* Initial Release
  * Added content [Wiki](https://github.com/AverageAnime/create-metalwork-multiloader/wiki/Content-List).
  * Compatibility [Wiki](https://github.com/AverageAnime/create-metalwork-multiloader/wiki/Compatibility).
  * Config options [Wiki](https://github.com/AverageAnime/create-metalwork-multiloader/wiki/Config-Options).
  * Processing [Wiki](https://github.com/AverageAnime/create-metalwork-multiloader/wiki/Processing-Guide).

> Please report all issues [here](https://github.com/AverageAnime/create-metalwork-multiloader/issues).
