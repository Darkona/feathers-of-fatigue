# Changelog

Changes by feature, newest version first.

## Unreleased

### For players and pack makers

- Armor weight is an attribute, `feathers_of_fatigue:armor_weight` (in feathers, reserved from the maximum): worn armor and weight sources add to it as Feathers' own modifier, and any item, effect, mod or `/attribute` can add weight with an ordinary attribute modifier (a heavy backpack) or scale it.
- The stamina is saved in its own entity attachment, `feathers_of_fatigue:stamina`, instead of inside `feathers_of_fatigue:feathers`. Old saves move over the first time a player or a mount loads; nothing is lost.
- Client and server must run the same version: the network protocol changed.

### For mod developers

- New attribute holder `FeathersAttributes.ARMOR_WEIGHT`. Nothing else in the API changed.

## 1.21.1-1.0.1 (NeoForge), 2026-10-01

Same as 1.21.1-1.0.0 below, except for what this section lists.

### For mod developers

- Another mod can take over player actions through the API: `FeathersAPI.takeOverPlayerActions(modId)`, called during mod construction or common setup, turns the basic exertion (the sprint and jump costs) off, so a player never pays twice for the same action. `FeathersAPI.arePlayerActionsTakenOver()` and `FeathersAPI.getPlayerActionOwners()` read it. The comment of `basic_exertion_enabled` says so.
- The API jar is `com.darkona.feathersoffatigue:feathers-of-fatigue-api:1.21.1-1.0.1`.

## 1.21.1-1.0.0 (NeoForge), 2026-10-01

A rewrite of Green Feathers for NeoForge 1.21.1 (21.1.252 or later), with a new API.

### Renamed to Feathers of Fatigue

- Green Feathers is now **Feathers of Fatigue**. Every id follows: the mod id and resource namespace are `feathers_of_fatigue` (attributes, effects, enchantments, tags, data maps, lang keys), the code lives in `com.darkona.feathersoffatigue` (the API in `com.darkona.feathersoffatigue.api`), and the jars are `feathers-of-fatigue` and `feathers-of-fatigue-api` (group `com.darkona.feathersoffatigue`).
- Config files are `FeathersOfFatigue-Server.toml`, `FeathersOfFatigue-Compat.toml` and `FeathersOfFatigue-Client.toml`, in `serverconfig/feathers_of_fatigue/` and `config/feathers_of_fatigue/`. The `/feathers` command keeps its name.
- Worlds and configs from earlier builds do not carry over: stored feathers, enchantments and items under the old ids are lost, and the old config files are ignored.

### Stamina

- Feathers regenerate on their own after a short pause once you spend some.
- **Strain:** when you run out you can keep going into red strain feathers. Regeneration pays strain back first, slowly.
- **Exhaustion:** spend absolutely everything and you can't exert yourself until you catch your breath.
- **Resting:** standing still, crouching or sitting (boats, horses, most furniture seats) pays strain back faster. Sleeping through the night restores everything, also when a sleep mod skips the night; leaving the bed before morning does not.
- **Food (optional):** a full food bar with saturation left speeds regeneration up, hunger slows it down (`saturation_regen_bonus`, `hunger_regen_penalty`). Off by default.

### Weather and climate

- Cold slows regeneration. Heat makes everything cost double; the Nether, fire and lava also cut your maximum feathers.
- Fire Resistance and the Potion of Cooling keep you fresh.

### Armor weight (optional)

- Every armor piece holds back feathers you can't use, drawn in the piece's own color, head to feet.
- Weights per item or material, set in the config or by datapack (data map).
- The Lightweight enchantment and the Feather Ring (Curios ring slot) lighten the load.
- Other mods can add weight (a backpack, a full inventory) through the API, drawn in their own color.

### Mounts (optional)

- Horses, donkeys, mules and camels have their own feathers, shown instead of yours while you ride, in the animal's colors.
- Galloping and jumping tire them; an exhausted mount slows down and can't jump.
- An animal that stops counting as a mount (config or datapack change) drops its strain, exhaustion and slowdown.
- Each animal is born with its own stamina, and foals take after their parents.
- Horse armor weighs a little.
- Which animals count, and their stats, set by datapack.

### Potions

- Endurance (golden bonus feathers; another potion extends what is left, a stronger one adds its extra feathers), Energy (faster regeneration), Momentum (cheaper actions), Cooling.

### HUD

- Feathers above the food bar, colored by state: normal, cold, hot, strained, energized, momentum, endurance.
- Feathers beyond one row stack in layers of stronger shades.
- Weight feathers drawn in each source's color.
- Every feather is drawn from grayscale sprites (body, half body, outline, shine, empty slot) tinted with a body and an outline color, instead of a hand-drawn set per color. Each state keeps its own shape: crystal feathers when cold or with Momentum, a glint when energized, its own stripe for strain.
- Cold puts frost over the feathers, as in the first Green Feathers. The flames overlay is available to styles and resource packs.
- The feathers move like hearts: a wave while Energized, a shake when only a few are left, a pulse (or a shake) while strained. Each is set in the client config (`[animations]`), and the low threshold too.
- Resource packs can recolor any state, change its shape or overlay, or give it sprites of their own, with `assets/feathers_of_fatigue/feather_styles.json` (see the Resource Packs wiki page).
- Armor tooltips show the item's weight.

### Compatibility

- Cold Sweat, Tough As Nails, Legendary Survival Overhaul: their body temperature decides cold and heat; their thirst or hydration affects regeneration.
- Droplets of Thirst, the maintained continuation of Thirst Was Taken: thirst slows regeneration, being well quenched speeds it up, and regenerating can cost thirst (optional). Players with thirst turned off are not affected.
- Thirst Was Taken: the same, for packs still on the original mod.
- Serene Seasons: winter outdoors is cold, summer sun is hot.
- Curios: the Feather Ring goes in a ring slot.
- AppleSkin, Overflowing Bars, Jade: sit nicely alongside the feathers.
- Each supported mod declares the versions it accepts; a version outside them stops the game at load with a message instead of a crash in play.

### Configuration

- Game rules live in a server config, synced to clients; modpacks ship defaults in `defaultconfigs/`.
- The `/feathers` command reads and changes a player's feathers.

### For mod developers

- A separate API jar: spend, drain and read feathers, add regeneration factors and weight sources, and listen to events.
- Feather styles (`com.darkona.feathersoffatigue.api.client`): register a `FeatherStyle` (body and border color, variant, overlay, optional sprites) and a `FeatherStyleProvider` that picks it for the player by condition, by priority. New shapes and overlays with `FeatherVariants`, from Feathers of Fatigue's 56x72 sheet layout or a texture of any size in multiples of 8. Feathers of Fatigue's own states use the same registry.
- Feather animations: a `FeatherAnimationProvider` picks a wave, shake or pulse for the row (the player's or their mount's) by condition, by priority. Feathers of Fatigue's own triggers use the same path.
- The old API of Elenai's Feathers (`com.elenai.feathers.api.FeathersHelper`) is gone: use `FeathersAPI` on the server or `ClientFeathers` on the client.

## Planned

- Nothing for 1.21.1 at the moment.
