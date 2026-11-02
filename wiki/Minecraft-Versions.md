# Minecraft Versions

This wiki describes Feathers of Fatigue for Minecraft 1.21.1 (NeoForge). The same mod is also made for Minecraft 1.20.1 (Forge), with the same features, config options and API, as far as the game allows. This page lists what is different on 1.20.1.

| Minecraft | Loader | Download | Differences |
|---|---|---|---|
| 1.21.1 | NeoForge 21.1.252 or later | [1.21.1-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.21.1-1.0.1) | None: this whole wiki |
| 1.20.1 | Forge 47.4.10 or later | [1.20.1-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.20.1-1.0.1) | [1.20.1](#1201-forge-47) |

Files and folders are the same in both versions:

- `serverconfig/feathers_of_fatigue/FeathersOfFatigue-Server.toml` and `FeathersOfFatigue-Compat.toml` for the game rules (synced to clients).
- `config/feathers_of_fatigue/FeathersOfFatigue-Client.toml` for the HUD.
- `defaultconfigs/feathers_of_fatigue/` for modpack defaults.

**Tested with, on 1.21.1:** Cold Sweat 2.4.5, Tough As Nails 10.1.0.13, Legendary Survival Overhaul 2.4.7.2, Droplets of Thirst 1.21.1-1.0.0, Thirst Was Taken 1.21.1-2.1.5, Serene Seasons 10.1.0.9, Curios 9.5.1, Jade 15.10.6, Overflowing Bars 21.1.1, Naturalist and Mob Wrangler.

## 1.20.1 (Forge 47)

**Missing, compared with 1.21.1:**

- **Mob Wrangler:** it has no Forge 1.20.1 build, so its rideable creatures get no feathers.
- **Enchantments from datapacks:** 1.20.1 has no data-driven enchantments. Lightweight and the Curse of Heaviness are defined in code. Datapacks cannot change their levels, costs or the items they apply to.
- **New mounts after `/reload`:** a creature gets its feathers when the game creates it. If a reload turns a creature type into a mount, its creatures get feathers after their chunk reloads, or after a restart.

**Tested with:** Cold Sweat 2.4.3.2, Tough As Nails 9.2, Legendary Survival Overhaul 2.4.7, Droplets of Thirst 1.20.1-1.0.0, Thirst Was Taken 1.4.0, Serene Seasons 9.1, Curios 5.14.1, Jade 11.13.3, AppleSkin 2.5.1, Overflowing Bars 8.0.1, Naturalist 5.0.

For the API differences on 1.20.1, see [Mod Developers](Mod-Developers#minecraft-1201).
