# How It Plays

## Feathers

Feathers sit above the food bar, two per icon, like hearts. A full row is 20 feathers. More feathers are drawn as layers over the same row, each a shade deeper (or lighter) than the one below, with a count next to it.

Feathers come back on their own, a little every second. After you spend some, there is a short pause before they start to come back. Several spends in a row make the pause longer, up to a limit.

You pick the color in the client config: green, blue (like Elenai's original) or white (chicken feathers).

| ![Green color](images/color-green.png) | ![Blue color](images/color-blue.png) | ![White color](images/color-white.png) |
|---|---|---|
| Green | Blue | White |

![More than 20 feathers, layered](images/layers.png)

*More than 20 feathers: a second layer over the first, and the count on the right.*

## Strain

When you run out, you can keep going into red *strain* feathers, up to a limit (6 by default). Regeneration pays strain back before anything else, and slowly. If you overdo it, you stay drained for a while.

![Two red strain feathers](images/strain.png)

## Exhaustion

If you spend absolutely everything (no feathers and no strain room left), you are exhausted. You cannot exert yourself again until you regain part of your bar (30% by default) with no strain left.

![Exhausted: the bar is spent and the strain is full](images/exhausted.png)

## Resting

Standing still, crouching or sitting down (a boat, a horse, or most seats from furniture mods) pays strain back faster. Sleeping through the night restores everything, also when a sleep mod skips the night for you. If you leave the bed before morning, sleep restores nothing.

## Food

Feathers come back at normal speed with a full food bar, and slower as it empties. Saturation adds a bonus on top: the more of it you have, the faster they return, up to 50% faster.

Recovering feathers also costs food, the same way healing does. At 6 hunger points (3 drumsticks) or less, feathers stop coming back until you eat. Only players eat, so mounts are not affected.

Server owners can change all of this: `regen_uses_hunger` turns the food cost and the stop off, and `saturation_regen_bonus` and `hunger_regen_penalty` set how much saturation and hunger change the speed.

With Droplets of Thirst installed, recovering feathers costs thirst too.

## Weather and climate

- **Cold** weather slows your recovery.
- **Heat** makes everything cost double.
- **The Nether, fire and lava** also cut your maximum feathers (fatigue).
- **Fire Resistance** or a **Potion of Cooling** keeps you fresh.

With a temperature or seasons mod installed, that mod decides when you are cold or hot (see [Compatibility](Compatibility)).

| ![Hot](images/hot.png) | ![Cold](images/cold.png) | ![Hot and fatigued](images/fatigued.png) |
|---|---|---|
| Hot: everything costs double | Cold: slower recovery | Fatigued: 4 feathers fewer |

## Armor weight (optional)

Every armor piece holds back some feathers that you cannot use. They are drawn from the right, head to feet, each in the color of its piece (leather in its dye). Netherite is heavy.

- The **Lightweight** enchantment takes 25% off a piece per level.
- The **Feather Ring** halves the weight. It goes in a Curios ring slot, or in the off hand without Curios.
- Other mods can add weight of their own, such as a backpack. It is drawn after the boots, in its own colors.

To turn it on, set `armor_weights_enabled` in the server config.

![An iron helmet, a diamond chestplate, gold leggings and red leather boots](images/armor.png)

*Iron helmet, diamond chestplate, gold leggings and red leather boots: each piece holds back feathers in its own color.*

## Mounts (optional)

Horses, donkeys, mules and camels have their own feathers. While you ride, their feathers replace yours, in the colors of the animal. Galloping and jumping tire them slowly. An exhausted mount slows down and cannot jump. If the mount cannot pay for a jump, the jump does not happen.

Each animal is born with its own stamina (14 to 30 feathers by default), like speed and health, and foals take after their parents. Horse armor weighs a little: leather, copper and gold 1 feather, iron and diamond 2.

Modpacks can give feathers to other creatures (see [Modpack Makers](Modpack-Makers)).

| ![Riding a white horse](images/mount-white.png) | ![Riding a black horse](images/mount-black.png) |
|---|---|
| A white horse | A black horse |
| ![A horse in diamond armor](images/mount-armor.png) | ![Riding a camel](images/mount-camel.png) |
| Diamond horse armor weighs 2 feathers | A camel |

## Potions

| Potion | Effect |
|---|---|
| Endurance | A row of golden bonus feathers on top of your own. Another drink extends what is left instead of refilling it |
| Energy | Faster recovery |
| Momentum | Cheaper actions |
| Cooling | Protects from heat |
| Coldness | Makes you cold: slower recovery |
| Heat | Makes you hot: everything costs double |

| ![Endurance](images/endurance.png) | ![Energized](images/energized.png) | ![Momentum](images/momentum.png) |
|---|---|---|
| Endurance | Energized | Momentum |
