### ***3.0.0***

---

### Overview

Addons are code-free resources that contributes config defaults, allowing any easy way to register content under the Create: Metalwork namespace and to set any option.

Third-party addons are located at `data/createmetalwork/addon.json`.

| Field             | Notes                                                                                                                                                 |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `spec_version`    | Current version: 1.0.                                                                                                                                 |
| `addon_id`        | Identifies the addon, and is what the player writes in `addons_enabled`. Falls back to the mod id. Two addons claiming one id: the second is ignored. |
| `name`            | Displayed name, shown in logs.                                                                                                                        |
| `default_enabled` | Whether the addon contributes is enabled by default.                                                                                                  |
| `config`          | Defaults for any option the mod defines, keyed by config file then by the option's dotted path.                                                       |

|          | Fields                                                                   | Notes                                                                                                                                                                                                                                                                                                           |
|----------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `fluids` | `id`, `slope`, `level`, `density`, `viscosity`, `tick_rate`, `condition` | Registers a molten metal under the `createmetalwork:` namespace. Each declaration also produces `flowing_<id>` and `<id>_bucket`. Omitted fields fall back to their defaults: slope `4`, level `2`, density `8000`, viscosity `70`, tick rate `15`. Requires new texture files; changes require a game restart. |
| `items`  | `id`, `max_stack`, `condition`                                           | Registers an item under the `createmetalwork:` namespace. `max_stack` defaults to `64` and must be between `1` and `64`. Requires new model/texture files; changes require a game restart.                                                                                                                      |

---

## Assets & Data

Recipes, models, blockstates, textures, loot tables and translations are ordinary resources that ship under `assets/createmetalwork/…` and `data/createmetalwork/…`. Use of tags and load conditions is also recommended.