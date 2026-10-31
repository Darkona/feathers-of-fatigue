# Minecraft Versions

This wiki describes the newest version of Feathers of Fatigue, for Minecraft 26.3 (NeoForge). The same mod is also made for older Minecraft versions. Each one has the same features, config options and API, as far as the game allows. This page lists only what is different in each version.

| Minecraft | Loader | Download | Differences |
|---|---|---|---|
| 26.3 | NeoForge 26.3.0.36-beta only | [26.3-1.0.0-beta.2](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v26.3-1.0.0-beta.2) (beta) | None: this whole wiki |
| 26.2 | NeoForge 26.2.0.88 or later | [26.2-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v26.2-1.0.1) | [26.2](#262-neoforge-262) |
| 26.1.2 | NeoForge 26.1.2.109 or later | [26.1.2-1.0.2](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v26.1.2-1.0.2) | [26.1.2](#2612-neoforge-2612) |
| 1.21.11 | NeoForge 21.11.45 or later | [1.21.11-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.21.11-1.0.1) | [1.21.11](#12111-neoforge-2111) |
| 1.21.1 | NeoForge 21.1.252 or later | [1.21.1-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.21.1-1.0.1) | [1.21.1](#1211-neoforge-211) |
| 1.20.1 | Forge 47.4.10 or later | [1.20.1-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.20.1-1.0.1) | [1.20.1](#1201-forge-47) |
| 1.19.2 | Forge 43.5.2 or later | [1.19.2-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.19.2-1.0.1) | [1.19.2](#1192-forge-43) |
| 1.18.2 | Forge 40.3.12 or later | [1.18.2-1.0.1](https://github.com/Darkona/feathers-of-fatigue/releases/tag/v1.18.2-1.0.1) | [1.18.2](#1182-forge-40) |

Files and folders are the same in every version:

- `serverconfig/feathers_of_fatigue/FeathersOfFatigue-Server.toml` and `FeathersOfFatigue-Compat.toml` for the game rules (synced to clients).
- `config/feathers_of_fatigue/FeathersOfFatigue-Client.toml` for the HUD.
- `defaultconfigs/feathers_of_fatigue/` for modpack defaults.

## 26.3 (NeoForge 26.3)

This version needs exactly NeoForge 26.3.0.36-beta. NeoForge 26.3 is in beta, and 26.3.0.37-beta renames the config types. Mods built for one side of that change (Feathers of Fatigue, Droplets of Thirst) do not load on the other.

**Missing:**

- **Tough As Nails:** it has no build for Minecraft 26.3, so its compat and config section are left out.
- **Cold Sweat, Thirst Was Taken, Legendary Survival Overhaul:** none of them has a build for Minecraft 26.x, so their compats and config sections are left out.
- **Mob Wrangler:** it has no build for Minecraft 26.x, so its mounts do not exist.

**Different:** the potions brew from data recipes in `data/feathers_of_fatigue/recipe/brewing/`, and datapacks can change them. On older versions, the mixes are in code.

**Tested with:** Droplets of Thirst 26.3-1.0.0-beta.1, Serene Seasons 26.1.2.0.7 (its build for 26.3), Curios 17.0.0-beta.2, Jade 26.3.1, AppleSkin 3.0.10, Overflowing Bars 26.3.0 with Puzzles Lib 26.3.8, Naturalist 2.0.6.

## 26.2 (NeoForge 26.2)

**Has, on top of 26.3:** Tough As Nails (with its `[tough_as_nails]` section in `FeathersOfFatigue-Compat.toml`).

**Missing:** Cold Sweat, Thirst Was Taken, Legendary Survival Overhaul and Mob Wrangler, as on 26.3.

**Tested with:** Droplets of Thirst 26.2-1.0.0, Tough As Nails 21.11.0.8, Serene Seasons 26.1.2.0.6 (its build for 26.2), Curios 16.0.0, Jade 26.2.10, AppleSkin 3.0.10, Overflowing Bars 26.2.0, Naturalist 2.0.6.

## 26.1.2 (NeoForge 26.1.2)

**Has, on top of 26.3:** Tough As Nails.

**Missing:** everything missing in 26.2, and:

- **Naturalist:** it has no build for Minecraft 26.1, so its mounts do not exist.

**Tested with:** NeoForge 26.1.2.109 and 26.1.2.112, Droplets of Thirst 26.1.2-1.0.2, Tough As Nails 21.11.0.6, Serene Seasons 26.1.2.0.4, Curios 15.0.0, Jade 26.1.11, AppleSkin 3.0.9, Overflowing Bars 26.1.0. Serene Seasons 26.1.2.0.7 crashes the client on its own. Use 26.1.2.0.4 to 26.1.2.0.6.

## 1.21.11 (NeoForge 21.11)

**Has, on top of 26.3:** Tough As Nails (with its `[tough_as_nails]` section in `FeathersOfFatigue-Compat.toml`).

**Missing:** Cold Sweat, Thirst Was Taken, Legendary Survival Overhaul, Naturalist and Mob Wrangler. None of them has a NeoForge build for Minecraft 1.21.11. Their compats and config sections are left out, and their mounts do not exist.

**Different:** the potions brew from mixes in code, as on 26.2 and older. Armor, its material rules and copper armor work as on 26.x. The API uses `Identifier`.

**Tested with:** Droplets of Thirst 1.21.11-1.0.0, Tough As Nails 21.11.0.4, Serene Seasons 21.11.0.5 with GlitchCore 21.11.0.4, Curios 14.0.0, Jade 21.1.7, AppleSkin 3.0.8, Overflowing Bars 21.11.0 with Puzzles Lib 21.11.13.

## 1.21.1 (NeoForge 21.1)

**Has, on top of 26.3:** Tough As Nails, Cold Sweat, Thirst Was Taken and Legendary Survival Overhaul (with their sections in `FeathersOfFatigue-Compat.toml`, see [Configuration](Configuration#default-feathersoffatigue-compattoml)), and Mob Wrangler mounts.

**Different:**

- **Armor materials:** material rules name the armor material, so the turtle shell is `@minecraft:turtle`. Copper armor does not exist, and the default `armor_weights` list has no copper entries.
- **API:** ids are `ResourceLocation`.
- **Food:** recovering feathers costs food by default, and with Droplets of Thirst that food drains thirst too. The speed follows the food bar and saturation. Other versions keep regeneration free by default.

**Tested with:** Cold Sweat 2.4.3.1, Tough As Nails 10.1.0.13, Legendary Survival Overhaul 2.4.7.2, Droplets of Thirst 1.21.1-1.0.0, Thirst Was Taken 1.21.1-2.1.5, Serene Seasons 10.1.0.9, Curios 9.5.1, Jade 15.10.6, Overflowing Bars 21.1.1, Naturalist and Mob Wrangler.

## 1.20.1 (Forge 47)

**Missing, compared with 1.21.1:**

- **Mob Wrangler:** it has no Forge 1.20.1 build, so its rideable creatures get no feathers.
- **Enchantments from datapacks:** 1.20.1 has no data-driven enchantments. Lightweight and the Curse of Heaviness are defined in code. Datapacks cannot change their levels, costs or the items they apply to.
- **New mounts after `/reload`:** a creature gets its feathers when the game creates it. If a reload turns a creature type into a mount, its creatures get feathers after their chunk reloads, or after a restart.

**Tested with:** Cold Sweat 2.4.3.2, Tough As Nails 9.2, Legendary Survival Overhaul 2.4.7, Droplets of Thirst 1.20.1-1.0.0, Thirst Was Taken 1.4.0, Serene Seasons 9.1, Curios 5.14.1, Jade 11.13.3, AppleSkin 2.5.1, Overflowing Bars 8.0.1, Naturalist 5.0.

## 1.19.2 (Forge 43)

**Missing:** everything missing in 1.20.1, and:

- **Camels and sniffers:** they do not exist in 1.19.2.
- **Legendary Survival Overhaul:** it has no 1.19.2 build, so its config section is left out.
- **Infinite effects:** 1.19.2 has no infinite durations. While their cause lasts, Cold, Heat, Fatigue and Strained last for years, and the inventory shows `**:**`.

**Tested with:** Cold Sweat 2.4.3, Tough As Nails 8.0, Droplets of Thirst 1.19.2-1.0.0, Thirst Was Taken 1.3.11, Serene Seasons 8.1, Curios 5.1.6, Jade 8.9.2, AppleSkin 2.4.2, Overflowing Bars 4.0.1, Naturalist 4.0.3.

## 1.18.2 (Forge 40)

Needs Forge 40.3.12 or later.

**Missing:** everything missing in 1.19.2, and:

- **Naturalist ostriches, giraffes and elephants:** Naturalist 1.1.1, its last 1.18.2 build, has no ostriches, and players cannot ride its giraffes and elephants. Its zebras are horses and have feathers.

**Tested with:** Cold Sweat 2.4.3, Tough As Nails 7.0, Droplets of Thirst 1.18.2-1.0.0, Thirst Was Taken 1.3.11, Serene Seasons 7.0, Curios 5.0.9, Jade 5.3.2, AppleSkin 2.5.1, Overflowing Bars 3.0.0, Naturalist 1.1.1.

For the API differences on older versions, see [Mod Developers](Mod-Developers#older-minecraft-versions).
