# Mod Developers

Other mods can spend feathers through the Feathers of Fatigue API. Compile against the API jar and treat it as optional:

```groovy
compileOnly "com.darkona.feathersoffatigue:feathers-of-fatigue-api:26.3-1.0.0-beta.2"
```

Guard every call with `ModList.get().isLoaded("feathers_of_fatigue")`. Keep the calls in a class that loads only when the mod is there. Your mod then works with and without Feathers of Fatigue.

Start with `com.darkona.feathersoffatigue.api.FeathersAPI`. Every class in the API has documentation.

## Spending

Costs are in stamina units. A feather is `Stamina.PER_FEATHER` (1000), so a cost can be a fraction of a feather.

```java
// A one-off cost. EXEMPT (creative players) also means "go ahead".
if (FeathersAPI.spend(player, MY_DASH, Stamina.ofFeathers(3)).allowed()) dash(player);

// A continuous cost: call every tick while it lasts, stop when it's refused.
if (!FeathersAPI.startDrain(player, MY_GLIDE, Stamina.perTick(1.5)).allowed()) stopGliding(player);
```

The source (`MY_DASH`) is your own `Identifier` (`ResourceLocation` on 1.21.1 and older). It shows in `/feathers debug` and in the spend events. With `SpendOptions`, a spend can never strain, be only a simulation, or set its own regeneration pause. `canSpend` checks without spending.

On the client, a spend predicts the result from the synced feathers. It uses the same price as the server: the usage multiplier and the stamina modifiers, with the same source. The decision of the server is final. `ClientFeathers.predictSpend` does the same for actions that the client decides. A drain started on the client runs there too, and each sync settles the result.

## Reading

`FeathersAPI.get(entity)` returns a `FeathersView`: feathers, maximum, strain, bonus, weight, exhaustion, rest state. Players and mounts with feathers have one. For every other entity, `hasFeathers()` returns `false`.

## Hooks

| Hook | For |
|---|---|
| `registerClimateProvider` | Temperature mods: decide when an entity is cold, hot, or overheating |
| `registerRegenFactor` | Thirst and similar: speed regeneration up or down |
| `registerStaminaModifier` | Change what a spend costs |
| `registerWeightSource` | Add weight of your own (a backpack, a full inventory) |
| `setRestBonus` / `removeRestBonus` | A hot spring or a bench that helps recovery |
| `addBonusStamina` / `removeBonusStamina` | Temporary bonus feathers |
| `blockRegen` / `unblockRegen` | Pause regeneration |
| `FeatherStyles.registerStyleProvider` | Recolor the feathers of the player by condition (client HUD) |
| `FeatherAnimations.registerProvider` | Animate the feather row by condition (client HUD) |

### Taking over player actions

Feathers of Fatigue has a basic exertion: sprinting and jumping cost feathers, so the mod does something on its own. If your mod charges player actions itself, sprinting and jumping included, tell Feathers of Fatigue during mod construction or common setup. The basic exertion then turns off, and a player never pays twice for the same action:

```java
FeathersAPI.takeOverPlayerActions(MyMod.MOD_ID);
```

The call cannot be undone. `FeathersAPI.arePlayerActionsTakenOver()` tells if a mod did it, and `FeathersAPI.getPlayerActionOwners()` lists which ones. Feathers of Fatigue 1.0.1 added this method (26.1.2-1.0.2 on 26.1.2, 26.3-1.0.0-beta.2 on 26.3): if you call it, ask for that version or newer.

### Weight sources

A weight source can name a color or an item. Its share of the weight is then drawn after the boots, in that color or in the colors of the item. Without either, it is drawn grey.

```java
FeathersAPI.registerWeightSource(MY_BACKPACK, new WeightSource() {
    @Override public double weight(LivingEntity entity) { return backpackWeight(entity); }
    @Override public Item displayItem(LivingEntity entity) { return MyItems.BACKPACK.get(); }
});
// Call FeathersAPI.recalculateWeight(entity) when the backpack's weight changes.
```

Weight is also an attribute, `feathers_of_fatigue:armor_weight`, in feathers reserved from the maximum. Worn armor and weight sources add to it. Any item, effect or mod can add weight or scale it with an ordinary attribute modifier, and `/attribute` works too.

## Feather styles

Every feather on the HUD is made of grayscale sprites, tinted with a `FeatherStyle`. A style has:

- A body color and a border color, both ARGB (`0xAARRGGBB`). The alpha multiplies with the fade of the HUD, and a border with alpha 0 is not drawn.
- A variant: the shape of the feather.
- Optionally, an overlay drawn over the whole row, in two colors of its own.

The classes are in `com.darkona.feathersoffatigue.api.client`. None of them touches client classes, so you can register from common setup.

Register a style under your own id, then a `FeatherStyleProvider` that picks it by condition. Feathers of Fatigue asks the providers once per client tick, highest priority first. The first id that is not null wins. If there is none, the HUD uses the color that the player configured. The states of Feathers of Fatigue itself (cold, hot, energized, momentum) answer at `FeatherStyles.STATUS_PRIORITY` (0). Register above it to win over them, or below it to recolor only the plain feathers.

```java
FeatherStyles.registerStyle(SICK, FeatherStyle.opaque(0x7BA05B, 0x2B1D0E).withVariant(FeatherVariants.CRYSTAL));
FeatherStyles.registerStyleProvider(SICK, 100, (player, feathers) -> player.hasEffect(MyEffects.SICKNESS) ? SICK : null);
```

`FeatherVariants` names the shapes (`FEATHER`, `CRYSTAL`, `GLINT`, `STRAINED`, `PLAIN`) and overlays (`FROST`, `HEAT`) of Feathers of Fatigue. For a shape of your own, draw a row of cells in a texture with the same layout as the Feathers of Fatigue sheet (see [Resource Packs](Resource-Packs)). Register it with `FeatherVariants.registerVariant(id, texture, row)`, or with `registerOverlay` for an overlay.

These methods expect a texture of the same size as the sheet, `FeatherVariants.SHEET_WIDTH` x `SHEET_HEIGHT` (56x72). For a texture of another size, give the size: `registerVariant(id, texture, row, width, height)`. Registration throws `IllegalArgumentException` unless all of these are true:

- Both sizes are multiples of 8. Pad the texture with transparent pixels if necessary.
- The texture is wide enough for the cells of the row: six for a variant, two for an overlay.
- The row fits in the height of the texture.

Feathers of Fatigue registers its own styles the same way, under the ids in `FeatherStyles` (`GREEN`, `COLD`, `STRAIN`, `ENDURANCE`, `ARMOR`, `EMPTY`, `EXHAUSTED`...). If you register one of those ids, your style replaces it. The `feather_styles.json` of a resource pack wins over what code registers, so players and packs have the last word. `FeatherStyle.CODEC` reads and writes the same JSON form, for mods with configs of their own.

Mount rows, armor pieces and colored weight sources keep the colors of their textures and items. A style provider picks only the feathers of the player.

## Feather animations

The row can move like the vanilla hearts: `WAVE` (a bump that runs along it, like Regeneration), `SHAKE` (random jitter, like low health) or `PULSE` (the feathers brighten and dim). A `FeatherAnimation` has a kind, a speed (1 is the default pace) and an amplitude. The amplitude is in pixels, or for `PULSE`, how far toward white, from 0 to 1.

A `FeatherAnimationProvider` picks one by condition, for the entity that the row shows: the local player, or the mount they ride. Feathers of Fatigue asks the providers once per client tick, highest priority first. The first answer that is not null wins. The triggers of Feathers of Fatigue itself answer at `FeatherAnimations.STATUS_PRIORITY` (0), in this order: strain, few feathers left, Energized. Players set each of them in the client config.

```java
FeatherAnimations.registerProvider(MY_FEVER, 50, (entity, feathers) -> entity.hasEffect(MyEffects.FEVER) ? FeatherAnimation.SHAKE : null);
```

## Events

These events are on the NeoForge event bus: `SpendEvent.Pre` (cancel or change a spend) and `SpendEvent.Post`, `GainEvent`, `DrainEvent`, `ExhaustionEvent`, `StrainEvent`, `RegenEvent`, and `ArmorWeightEvent` (change the total weight).

## Older Minecraft versions

The API (`feathers-of-fatigue-api`) is the same in every version, with the version of the mod jar: `1.21.1-1.0.1`, `1.20.1-1.0.1`, `26.1.2-1.0.2`... [Minecraft Versions](Minecraft-Versions) lists them all. On 1.21.1 and older, ids are the Minecraft `ResourceLocation`, where 26.x and 1.21.11 have `Identifier`. On 1.20.1 and older, Forge needs a few more changes:

- Attributes, effects and enchantments are `RegistryObject`s (`FeathersAttributes.MAX_FEATHERS.get()`), not `DeferredHolder`s.
- Events go on `MinecraftForge.EVENT_BUS`. `SpendEvent.Post#getSpendResult` replaces `getResult`, because the Forge `Event` already has a `getResult`.
- `FeathersDataMaps` holds the data map ids and reads them: `FeathersDataMaps.armorWeight(item)`, `FeathersDataMaps.mountStats(type)`. Feathers of Fatigue loads the data maps itself, from the same paths and in the same format.
- Attribute modifier operations have their old names: a Feather Ring is a `MULTIPLY_BASE` modifier of -0.5 on `feathers_of_fatigue:armor_weight_multiplier`.
- **1.18.2 only:** events are Forge 40 `LivingEvent`s. `getEntityLiving()` gives the entity.
