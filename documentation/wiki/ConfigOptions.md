### ***3.0.0***

---

**If you are updating to a version with new config defaults, you will need to regenerate the list, or add the new entries manually.**

---

# `createmetalwork-common.toml`

---

## Addons

### Enabled Addons
Format: `addon_id`
* Choose which addons contribute their content. The list holds the enabled ids, so delete to disable.
* Every installed addon is named in the log on startup.
* An addon shapes the defaults a config is written with. A value already in your toml wins, so turning an addon on or off changes an existing config only once you reset the lists it contributes to.
```toml
[addons]
addons_enabled = [""]
```

---

## Blocks

### Custom Fluids
Format: `name`, `name|slopeFindDistance|levelDecreasePerBlock`, or `name|slopeFindDistance|levelDecreasePerBlock|density|viscosity|tickRate`
> **Note: Custom fluids will require new texture files.**
* Register molten metals under the `createmetalwork:` namespace using the `fluid` list.
* `name` becomes the fluid's registry ID: `createmetalwork:<name>`. Each line registers the fluid and bucket item.
* Omitted fields fall back to their defaults: slope `4`, level `2`, density `8000`, viscosity `70`, tick rate `15`.
> **Note: Changes require a game restart.**

```toml
[blocks]
fluid = [
  "molten_iron|4|2|7800|70|15",
  "molten_aluminum|4|1|2700|60|11",
  "molten_mythril"
]
```

---

## Items

### Custom Items
Format: `name`, or `name|maxStack`
> **Note: Custom items will require new model/texture files.**
* Register items under the `createmetalwork:` namespace using the `item` list.
* `name` becomes the item's registry ID: `createmetalwork:<name>`.
* `maxStack` defaults to `64` and must be between `1` and `64`.
> **Note: Changes require a game restart.**
```toml
[items]
item = [
  "andesite_dust",
  "crushed_netherite_scrap",
  "my_alloy_dust|16"
]
```

---

## Registry Conditions
Format: `<entry>@<clause>`, chainable as `<entry>@<clause>@<clause>`
* Every line in `fluid` and `item` may end with a clause which decides whether it registers.
* The clause goes at the very end of the line, after any `|` fields.
> **Note: Changes require a game restart.**

| Clause             | Meaning                                                       |
|--------------------|---------------------------------------------------------------|
| `@absent:<modids>` | Skip registration if any of them is installed.                |
| `@mod:<modids>`    | Only register when all of them are installed.                 |
| `@any:<modids>`    | Several mods could supply it, and one is enough to register.  |
| `@always`          | Register unconditionally.                                     |

```toml
[blocks]
fluid = [
  "molten_iron|4|2|7800|70|15@absent:alltheores,createmetallurgy",
  "molten_uranium|4|2|19100|80|18@absent:alltheores@mod:mekanism"
]

[items]
item = [
  "crushed_raw_mythril@any:mythicmetals,create_dd",
  "crushed_raw_tungsten@any:createmetallurgy,northstar@absent:northstar"
]
```

---

# `createmetalwork-server.toml`

---

## Compatibility

### Create
- **expanded_basin_fluids**: Give Create's basin 4 fluid slots on each side instead of 2, allowing mixing recipes to call for more than two fluids and produce more than two (default: `true`). **Disabling discards any extra fluid, input or output**.
  * Input and output are raised along with the recipe caps. Each slot still holds 1000 mB.
```toml
[compat.create]
expanded_basin_fluids = true
```
