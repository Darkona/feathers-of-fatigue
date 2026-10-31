# Compatibility

Feathers of Fatigue stacks neatly with the other bars on the right: food, air bubbles, thirst.

| ![With Thirst Was Taken, Cold Sweat and AppleSkin](images/compat-thirst.png) | ![With Tough As Nails, Serene Seasons and AppleSkin](images/compat-tan.png) |
|---|---|
| Thirst Was Taken, Cold Sweat and AppleSkin | Tough As Nails, Serene Seasons and AppleSkin |
| ![Underwater in iron armor](images/underwater.png) | ![Jade showing a horse's stamina](images/jade.png) |
| Underwater in iron armor: feathers above the air bubbles | Jade shows the stamina of a mount |

These mods work out of the box. Each one has its own switch in `FeathersOfFatigue-Compat.toml`:

| Mod | What it does with feathers |
|---|---|
| Droplets of Thirst | Thirst slows recovery, and being well quenched speeds it up. Recovering feathers costs thirst |
| Serene Seasons | Winter outdoors is cold, and the summer sun is hot |
| Curios | The Feather Ring goes in a ring slot |
| Jade | Shows the stamina of a mount when you look at it |
| AppleSkin, Overflowing Bars | Sit nicely next to the feathers |

Older Minecraft versions also support these:

| Mod | What it does with feathers |
|---|---|
| Tough As Nails (26.2 and older) | Its temperature decides cold and heat. Its thirst slows or speeds up recovery |
| Cold Sweat (1.21.1 and older) | Your body temperature decides when you are cold or overheating |
| Legendary Survival Overhaul (1.21.1 and older, not 1.19.2) | The same as Tough As Nails, from its temperature and hydration |
| Thirst Was Taken (1.21.1 and older) | The same as Droplets of Thirst, for worlds still on the original mod (Droplets of Thirst is its maintained continuation) |

These mods have no build for the newer versions. [Minecraft Versions](Minecraft-Versions) lists what each version supports and was tested with.

## Versions

Feathers of Fatigue calls these mods directly. For this reason it accepts the versions it was tested with, up to the next major version. With a version outside that range, the game stops at load and names the mod and the range, so it does not crash later in the middle of play.

| Mod | Accepted versions (26.3) |
|---|---|
| Droplets of Thirst | 26.3-1.0.0-beta.1 up to 26.3-2 |
| Serene Seasons | 26.1.2.0.7 up to 26.1.3 (its builds for 26.x keep 26.1 numbers) |
| Curios | 17.0.0-beta.2 up to 18 |
| Jade | 26.3.1 up to 26.4 |
| Overflowing Bars | 26.3.0 up to 26.4 |

Droplets of Thirst has the same rule on every Minecraft version: from `<minecraft>-1.0.0` up to `<minecraft>-2`, where `<minecraft>` is the version that Feathers of Fatigue is made for. On 1.21.1, that is `1.21.1-1.0.0` up to `1.21.1-2`. On 26.3, where Droplets of Thirst is a beta too, it starts at `26.3-1.0.0-beta.1`.
