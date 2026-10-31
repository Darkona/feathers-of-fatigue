# Modpack Makers

Everything on this page works with a datapack. You do not need code.

## Defaults for your pack

Put your versions of `FeathersOfFatigue-Server.toml` and `FeathersOfFatigue-Compat.toml` in `defaultconfigs/feathers_of_fatigue/`. New worlds and servers start from them. [Configuration](Configuration) lists every option.

## Mounts

Horses, donkeys, mules and camels have feathers when `mounts_enabled` is on. To make any other creature a mount, give it a data map entry in `data/feathers_of_fatigue/data_maps/entity_type/mount_stats.json`. The entry can have its own numbers. A field that you leave out uses the config value:

```json
{
  "values": {
    "mymod:dragon": { "min_feathers": 40, "max_feathers": 60, "gallop_feathers_per_second": 0.05 }
  }
}
```

| Field | Meaning |
|---|---|
| `min_feathers`, `max_feathers` | The range for the random stamina of each animal when it spawns |
| `regen_feathers_per_second` | How fast the mount recovers |
| `gallop_feathers_per_second` | The cost of galloping |
| `gallop_speed` | The speed from which the mount counts as galloping (horizontal blocks per tick) |
| `jump_feathers` | The cost of a full-power jump |

The `feathers_of_fatigue:mounts` entity type tag also works. Creatures in it use the numbers from the config.

Feathers do not make a creature rideable. They only matter for creatures that players can already ride.

Feathers of Fatigue ships entries for mounts from several mods (Naturalist, Alex's Mobs, Mob Wrangler, Sniff Trail, Wyrmroost, and others). Each entry applies only when its mod is installed.

### Opting out

Entity types in the `feathers_of_fatigue:no_feathers` tag never have feathers, even horses. Use it for special mounts that must not get tired. The tag file is `data/feathers_of_fatigue/tags/entity_type/no_feathers.json`:

```json
{ "values": ["mymod:spirit_horse"] }
```

## Armor weights

Feathers of Fatigue looks for the weight of a piece in this order: an item rule, a tag rule, the `feathers_of_fatigue:armor_weight` data map, a material-and-piece rule, a material rule. If nothing matches, the weight is the defense points of the armor times `unlisted_armor_weight_per_defense`. The rules are in `armor_weights` in the server config:

```toml
armor_weights = [
    "minecraft:netherite_chestplate=5",   # one item
    "#c:armors/heavy=4",                  # an item tag
    "@minecraft:iron/chestplate=3",       # one piece of a material (helmet, chestplate, leggings, boots, body)
    "@minecraft:iron=2"                   # every piece of a material
]
```

`body` is horse armor. On Minecraft 26.x and 1.21.11, the material is the equipment model of the armor (the `asset_id` of its `minecraft:equippable` component). For vanilla armor, that is its material: `minecraft:turtle_scute` for the turtle shell, `minecraft:copper` for copper armor. On 1.21.1 and older, it is the armor material (`minecraft:turtle`).

Armor weight is also the attribute `feathers_of_fatigue:armor_weight`, in feathers, so items, effects and `/attribute` can add weight or scale it with ordinary attribute modifiers.

The data map sets weights per item from a datapack, in `data/feathers_of_fatigue/data_maps/item/armor_weight.json`:

```json
{
  "values": {
    "mymod:plate_armor_chestplate": 6
  }
}
```
