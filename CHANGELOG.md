# Changelog

Changes by feature, newest version first.

## 1.20.1-1.1.0 (Forge 47), Unreleased

The 1.1.0 changes of 1.21.1, on Forge 47. Same as 1.20.1-1.0.1 below, except for what this section lists.

### For players and pack makers

- Armor weight is an attribute, `feathers_of_fatigue:armor_weight` (in feathers, reserved from the maximum): worn armor and weight sources add to it as Feathers' own modifier, and any item, effect, mod or `/attribute` can add weight with an ordinary attribute modifier (a heavy backpack) or scale it.
- Client and server must run the same version: the network protocol changed.
- The mod has a new logo, shown in the Mods screen.
- The strong Energized tipped arrow now has the right English name, Arrow of Energy.
- Sprinting and jumping now move the feathers on the client right away, with the same code as the server, instead of waiting for the next sync.
- Recovering feathers now costs food by default, and they stop coming back at 6 hunger points or less. Turn it off with `regen_uses_hunger`. A whole bar of 20 feathers costs half a hunger point (`hunger_exhaustion_per_feather`, now 0.1).
- Feathers come back slower as the food bar empties, on a smooth scale from `hunger_regen_penalty` (now 0.5) to normal speed at a full bar. Saturation adds a bonus on top that grows with it, up to `saturation_regen_bonus` (now 1.5).
- With Droplets of Thirst 1.20.1-1.1.0 or later, the food spent on recovering feathers drains thirst too.
- Server configs that already exist keep their old values: change them by hand to get the new defaults.
- New switches for two effects: `effect_energized_enabled` and `effect_cooling_enabled`.
- Thirst Was Taken: the thirst spent on recovering feathers now shows on the client right away.
- Values set with `/feathers` or the API for maximum feathers, maximum strain and regeneration now survive death, as they do on 1.21.1.
- Tested with Cold Sweat 2.4.5.

### For mod developers

- New attribute holder `FeathersAttributes.ARMOR_WEIGHT`. On 1.20.1 weight is added with an `ADDITION` modifier. Nothing else in the API changed.
- The API jar is `com.darkona.feathersoffatigue:feathers-of-fatigue-api:1.20.1-1.1.0`.

### Not in this version

- **Stamina in its own attachment:** on 1.21.1 the stamina moved to the attachment `feathers_of_fatigue:stamina`, which comes from a library this branch does not use. Here it stays in the feathers capability, so stamina saved by earlier versions carries over.

## 1.20.1-1.0.1 (Forge 47), 2026-10-01

Same as 1.20.1-1.0.0 below, except for what this section lists.

### For mod developers

- Another mod can take over player actions through the API: `FeathersAPI.takeOverPlayerActions(modId)`, called during mod construction or common setup, turns the basic exertion (the sprint and jump costs) off, so a player never pays twice for the same action. `FeathersAPI.arePlayerActionsTakenOver()` and `FeathersAPI.getPlayerActionOwners()` read it. The comment of `basic_exertion_enabled` says so.
- The API jar is `com.darkona.feathersoffatigue:feathers-of-fatigue-api:1.20.1-1.0.1`.

## 1.20.1-1.0.0 (Forge 47), 2026-10-01

The Green Feathers rewrite of 1.21.1, ported to Forge 1.20.1 with the same features, config options and API. It replaces the 1.3.0 code of this branch, and its API. Needs Forge 47.4.10 or later.

### Renamed to Feathers of Fatigue

- Green Feathers is now **Feathers of Fatigue**. Every id follows: the mod id and resource namespace are `feathers_of_fatigue` (attributes, effects, enchantments, tags, lang keys), the code lives in `com.darkona.feathersoffatigue` (the API in `com.darkona.feathersoffatigue.api`), and the jars are `feathers-of-fatigue` and `feathers-of-fatigue-api` (group `com.darkona.feathersoffatigue`).
- Config files are `FeathersOfFatigue-Server.toml`, `FeathersOfFatigue-Compat.toml` and `FeathersOfFatigue-Client.toml`, in `serverconfig/feathers_of_fatigue/` and `config/feathers_of_fatigue/`. The `/feathers` command keeps its name.
- Worlds and configs from earlier builds do not carry over: stored feathers, enchantments and items under the old ids are lost, and the old config files are ignored.

### Stamina

- Feathers regenerate on their own after a short pause once you spend some.
- When you run out you can keep going into red strain feathers. Regeneration pays strain back first, slowly.
- **Exhaustion:** spend absolutely everything and you can't exert yourself until you catch your breath.
- **Resting:** standing still, crouching or sitting (boats, horses, most furniture seats) pays strain back faster. Sleeping through the night restores everything, also when a sleep mod skips the night; leaving the bed before morning does not.
- **Food (optional):** a full food bar with saturation left speeds regeneration up, hunger slows it down (`saturation_regen_bonus`, `hunger_regen_penalty`). Off by default.
- Sprinting and jumping cost feathers, and other mods can spend them through the API.

### Weather and climate

- Cold slows regeneration. Heat makes everything cost double; the Nether, fire and lava also cut your maximum feathers.
- Fire Resistance and the Potion of Cooling keep you fresh.

### Armor weight (optional)

- Every armor piece holds back feathers you can't use, drawn in the piece's own color (leather in its dye), head to feet.
- Weights per item, tag or material, set in the config or by datapack in `data/feathers_of_fatigue/data_maps/item/armor_weight.json`.
- Horse armor weighs too. It has no armor material in 1.20.1, so material rules match its texture name: `horse_armor_iron` is `@minecraft:iron/body`.
- The Lightweight enchantment and the Feather Ring (Curios ring slot, or the off hand without Curios) lighten the load; the Curse of Heaviness doubles it.
- Other mods can add weight (a backpack, a full inventory) through the API, drawn in their own color.

### Mounts (optional)

- Horses, donkeys, mules and camels have their own feathers, shown instead of yours while you ride, in the animal's colors.
- Galloping and jumping tire them; an exhausted mount slows down and can't jump.
- An animal that stops counting as a mount (config or datapack change) drops its strain, exhaustion and slowdown.
- Each animal is born with its own stamina, and foals take after their parents.
- Which animals count, and their stats, set by datapack in `data/feathers_of_fatigue/data_maps/entity_type/mount_stats.json` and the `feathers_of_fatigue:mounts` and `feathers_of_fatigue:no_feathers` entity type tags (`data/feathers_of_fatigue/tags/entity_types/`).

### Potions

- Endurance (golden bonus feathers; another potion extends what is left, a stronger one adds its extra feathers), Energy (faster regeneration), Momentum (cheaper actions), Cooling, Coldness and Heat.

### HUD

- Feathers above the food bar, colored by state: normal, cold, hot, strained, energized, momentum, endurance. Green, blue or white, your choice.
- Feathers beyond one row stack in layers of stronger shades.
- Weight feathers drawn in each source's color.
- Every feather is drawn from grayscale sprites (body, half body, outline, shine, empty slot) tinted with a body and an outline color, instead of a hand-drawn set per color. Each state keeps its own shape: crystal feathers when cold or with Momentum, a glint when energized, its own stripe for strain.
- Cold puts frost over the feathers, as in the first Green Feathers. The flames overlay is available to styles and resource packs.
- The feathers move like hearts: a wave while Energized, a shake when only a few are left, a pulse (or a shake) while strained. Each is set in the client config (`[animations]`), and the low threshold too.
- Resource packs can recolor any state, change its shape or overlay, or give it sprites of their own, with `assets/feathers_of_fatigue/feather_styles.json` (see the Resource Packs wiki page).
- Armor and horse armor tooltips show the item's weight.

### Compatibility

Each one only does something when its mod is installed, and can be turned off in the config. Works with these 1.20.1 Forge builds, and each accepts versions up to its next major one: another version stops the game at load with a message instead of a crash in play.

- Cold Sweat 2.4.3.2: body temperature decides cold and heat.
- Tough As Nails 9.2.0.171: its temperature decides cold and heat; its thirst affects regeneration.
- Legendary Survival Overhaul 1.20.1-2.4.7: its temperature and hydration, the same way.
- Droplets of Thirst 1.20.1-1.0.0, the maintained continuation of Thirst Was Taken: thirst slows regeneration, being well quenched speeds it up, and regenerating can cost thirst (optional, `thirst_per_regenerated_feather` in `[droplets_of_thirst]`). Players with thirst turned off are not affected.
- Thirst Was Taken 1.20.1-1.4.0: the same, for packs still on the original mod.
- Serene Seasons 9.1.0.3: winter outdoors is cold, summer sun is hot.
- Curios 5.14.1: the Feather Ring goes in a ring slot.
- Jade 11.13.3: looking at a mount shows its stamina.
- AppleSkin 2.5.1, Overflowing Bars 8.0.1: sit nicely alongside the feathers.
- Mounts from other mods listed in the mount stats data map get feathers when those mods are installed (works with Naturalist 5.0pre2).

### Configuration

- Game rules live in server configs (`serverconfig/feathers_of_fatigue/FeathersOfFatigue-Server.toml` and `FeathersOfFatigue-Compat.toml`), synced to clients; modpacks ship defaults in `defaultconfigs/feathers_of_fatigue/`. HUD options are in `config/feathers_of_fatigue/FeathersOfFatigue-Client.toml`. Option names are the same as in 1.21.1.
- The `/feathers` command reads and changes a player's or a mount's feathers.

### For mod developers

- A separate API jar, `feathers-of-fatigue-api`: spend, drain and read feathers, add regeneration factors, climate providers, stamina modifiers and weight sources, and listen to events.
- Feather styles (`com.darkona.feathersoffatigue.api.client`): register a `FeatherStyle` (body and border color, variant, overlay, optional sprites) and a `FeatherStyleProvider` that picks it for the player by condition, by priority. New shapes and overlays with `FeatherVariants`, from Feathers of Fatigue's 56x72 sheet layout or a texture of any size in multiples of 8. Feathers of Fatigue's own states use the same registry.
- Feather animations: a `FeatherAnimationProvider` picks a wave, shake or pulse for the row (the player's or their mount's) by condition, by priority. Feathers of Fatigue's own triggers use the same path.
- The old API of Elenai's Feathers (`com.elenai.feathers.api.FeathersHelper`) is gone: use `FeathersAPI` on the server or `ClientFeathers` on the client.
- The same API as 1.21.1, with the changes Forge 1.20.1 needs:
  - Attributes, effects and enchantments are `RegistryObject`s (`FeathersAttributes.MAX_FEATHERS.get()`), not `DeferredHolder`s.
  - Events go on `MinecraftForge.EVENT_BUS`. `SpendEvent.Post#getSpendResult` replaces `getResult`, which Forge's `Event` already has.
  - `FeathersDataMaps` holds the data map ids and reads them: `FeathersDataMaps.armorWeight(item)`, `FeathersDataMaps.mountStats(type)`.
  - Attribute modifier operations have their 1.20.1 names: a Feather Ring is a `MULTIPLY_BASE` modifier of -0.5 on `feathers_of_fatigue:armor_weight_multiplier`.

### Not in this version

- **Mob Wrangler:** there is no Forge 1.20.1 build, so its rideable creatures get no feathers. Its entry in the mount stats data map stays inactive.
- **Enchantments from datapacks:** 1.20.1 has no data-driven enchantments. Lightweight and the Curse of Heaviness are defined in code: datapacks can't change their levels, costs or the items they apply to.
- **New mounts after `/reload`:** Forge 1.20.1 attaches feathers to a creature when it is created. A creature type that a reload turns into a mount gets its feathers only after its chunk reloads (or the server restarts).

## Planned

- Nothing for 1.20.1 at the moment.
