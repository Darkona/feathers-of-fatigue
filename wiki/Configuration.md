# Configuration

Feathers of Fatigue has three config files in `config/feathers_of_fatigue/`:

| File | Kind | Contents |
|---|---|---|
| `FeathersOfFatigue-Server.toml` | Server | Feathers, regeneration, strain, exhaustion, effects, resting, armor weights, basic exertion, mounts |
| `FeathersOfFatigue-Compat.toml` | Server | One section for each supported mod, each with its own switch |
| `FeathersOfFatigue-Client.toml` | Client | The HUD: position, feather color, animations, tooltips |

The server configs belong to the server. It sends its own to every player who joins, so everyone plays by the same rules, whatever their own files say. Modpacks ship their defaults in `defaultconfigs/`. Each player has their own client config.

Changes to the server configs apply while the game runs.

## Commands

Operators (permission level 2) can examine and change feathers:

| Command | What it does |
|---|---|
| `/feathers info <targets>` | Shows the feathers, strain, weight and state of the targets |
| `/feathers set <targets> <amount>` | Sets their feathers. Above zero, it also clears their strain |
| `/feathers reset <targets>` | Full feathers, no strain, not exhausted |
| `/feathers max <targets> <amount>` | Sets their base maximum feathers (kept across rejoins and deaths, like any attribute base) |
| `/feathers regen <targets> <amount>` | Sets their base regeneration, in feathers per second (kept across rejoins and deaths) |
| `/feathers spend <targets> <amount>` | Spends feathers, as an action does |
| `/feathers debug <targets> [seconds]` | Lists what spent their feathers recently, by source |

Targets can also be mounts.

## Default `FeathersOfFatigue-Server.toml`

```toml
[general]
	#Feathers a player has. Two feathers make one icon, like hearts: 20 is a full row.
	#This is the base of the feathers_of_fatigue:max_feathers attribute; effects and items modify it.
	# Default: 20
	# Range: 0 ~ 1000
	max_feathers = 20
	#Feathers regenerated per second. Base of the feathers_of_fatigue:feathers_per_second attribute;
	#Cold halves it, Energized doubles it. Any value works, however small: fractions carry over.
	# Default: 0.4
	# Range: -40.0 ~ 40.0
	regen_feathers_per_second = 0.4
	#Sleeping through the night restores all feathers and clears strain and exhaustion.
	sleeping_restores_all_feathers = true
	#Ticks without regeneration after spending feathers, for spends that don't set their own.
	# Default: 30
	# Range: 0 ~ 1200
	default_usage_cooldown_ticks = 30
	#Cooldowns from consecutive spends add up to at most this many seconds.
	# Default: 5
	# Range: 0 ~ 600
	max_cooldown_seconds = 5
	#Regenerating feathers costs food, like healing: no regeneration at 6 hunger points or less.
	regen_uses_hunger = true
	#Food exhaustion per regenerated feather when regen_uses_hunger is on. 4.0 exhaustion = one hunger point.
	# Default: 0.1
	# Range: 0.0 ~ 40.0
	hunger_exhaustion_per_feather = 0.1
	#Regeneration multiplier at maximum saturation: 1.5 regenerates 50% faster.
	#It grows in a straight line with saturation, on top of the food bar. 1.0 turns it off.
	# Default: 1.5
	# Range: 1.0 ~ 10.0
	saturation_regen_bonus = 1.5
	#Regeneration multiplier at 6 hunger points or less: 0.5 regenerates at half speed, 0.0 stops it.
	#It rises in a straight line to normal speed at a full food bar. 1.0 turns it off.
	#With regen_uses_hunger on, regeneration stops at 6 hunger points or less anyway.
	# Default: 0.5
	# Range: 0.0 ~ 1.0
	hunger_regen_penalty = 0.5

[exhaustion_and_strain]
	#Strain: when feathers run out, keep exerting by overspending into red 'negative' feathers, up to
	#max_strained_feathers. Regeneration pays the strain back first, slowly; resting speeds it up.
	#Mods can still ask for a spend that never strains.
	strain_enabled = true
	#How far into strain a player can go, in feathers. Base of the feathers_of_fatigue:max_strain attribute.
	# Default: 6
	# Range: 1 ~ 1000
	max_strained_feathers = 6
	#Exhaustion: once a player has nothing left to spend (no feathers, and no strain room when strain is
	#on), they are exhausted and can't exert again until they recover exhaustion_recovery of the bar.
	#Off: they can spend again as soon as anything regenerates.
	exhaustion_enabled = true
	#Share of the maximum feathers to regain, with no strain left, before exhaustion ends.
	# Default: 0.3
	# Range: 0.0 ~ 1.0
	exhaustion_recovery = 0.3

[effects]
	#Cold effect: halves regeneration (level II stops it). Applied in cold, snowy or freezing places.
	effect_cold_enabled = true
	#Biome temperature below which rain or snow applies Cold. Cold biomes go from 0.05 down to -0.7.
	# Default: -0.3
	# Range: -2.0 ~ 2.0
	cold_temperature = -0.3
	#Heat effect: doubles costs. First heat tier: a hot biome under the sun, or a temperature mod's heat.
	#Fire Resistance and the Cooling effect prevent it.
	effect_hot_enabled = true
	#Biome temperature from which being under the sun applies Heat. The hottest biomes reach 2.0.
	# Default: 1.8
	# Range: -2.0 ~ 4.0
	hot_temperature = 1.8
	#Fatigue effect: 4 fewer max feathers per level. Second heat tier, on top of Heat: the Nether, burning,
	#lava, or a temperature mod's severe heat. Other mods may apply it too. Fire Resistance and Cooling prevent it.
	effect_fatigue_enabled = true
	#Being in the Nether counts as severe heat.
	fatigue_from_nether = true
	#Being on fire or in lava counts as severe heat.
	fatigue_from_burning = true
	#Ticks that Cold, Heat and Fatigue last after leaving what caused them. 0 disables lingering.
	# Default: 60
	# Range: 0 ~ 12000
	effect_lingering_ticks = 60
	#Endurance effect: golden feathers spent before regular ones; the effect ends when they run out.
	effect_endurance_enabled = true
	#Momentum effect: halves costs.
	effect_momentum_enabled = true
	#Energized effect: doubles regeneration.
	effect_energized_enabled = true
	#Cooling effect: protects from Heat and Fatigue.
	effect_cooling_enabled = true

[resting]
	#Resting speeds up paying back strain: standing still, crouching still, or sitting (riding a boat,
	#a mount, or a seat from another mod). Mods can add rest bonuses through the API; the best one applies.
	rest_enabled = true
	#Ticks without moving before standing still counts as resting. 20 ticks = 1 second.
	# Default: 40
	# Range: 1 ~ 1200
	rest_still_ticks = 40
	#Strain recovery multiplier while standing still.
	# Default: 1.5
	# Range: 1.0 ~ 20.0
	rest_still_multiplier = 1.5
	#Strain recovery multiplier while crouching still.
	# Default: 2.5
	# Range: 1.0 ~ 20.0
	rest_crouching_multiplier = 2.5
	#Strain recovery multiplier while sitting.
	# Default: 2.0
	# Range: 1.0 ~ 20.0
	rest_sitting_multiplier = 2.0
	#Resting also speeds up normal regeneration, not only strain recovery.
	rest_boosts_regen = false

[armor_weights]
	#Worn armor has weight: each point makes one feather unusable (shown grey on the HUD).
	armor_weights_enabled = false
	#Weight rules, as 'target=weight'. The most specific matching rule wins:
	#  minecraft:iron_chestplate=3      one item
	#  #mymod:heavy_armor=6            every item in an item tag
	#  (the feathers_of_fatigue:armor_weight data map, which mods and datapacks can ship, comes here)
	#  @minecraft:iron/chestplate=3    one piece of an armor material (helmet, chestplate, leggings, boots, body for horse armor)
	#  @minecraft:iron=2               every piece of an armor material
	#Armor that matches nothing weighs its defense points times unlisted_armor_weight_per_defense.
	armor_weights = ["@minecraft:leather=1", "@minecraft:chainmail=1", "@minecraft:turtle_scute=1", "@minecraft:copper=1", "@minecraft:gold=2", "@minecraft:iron=2", "@minecraft:diamond=3", "@minecraft:netherite=4", "@minecraft:leather/body=1", "@minecraft:copper/body=1", "@minecraft:gold/body=1", "@minecraft:iron/body=2", "@minecraft:diamond/body=2"]
	#Weight of armor no rule or data map covers, per point of defense. 0 makes it weightless.
	# Default: 0.5
	# Range: 0.0 ~ 10.0
	unlisted_armor_weight_per_defense = 0.5
	#Share of a piece's weight each level of Lightweight removes: 0.25 leaves 25% at level III.
	# Default: 0.25
	# Range: 0.0 ~ 1.0
	lightweight_reduction_per_level = 0.25

[basic_exertion]
	#Sprinting and jumping cost feathers, so Feathers of Fatigue does something on its own.
	#Always off when another mod takes over player actions through the API.
	basic_exertion_enabled = true
	#Feathers per second while sprinting. Regeneration pauses while sprinting.
	# Default: 1.0
	# Range: 0.0 ~ 40.0
	sprint_feathers_per_second = 1.0
	#Feathers per jump.
	# Default: 0.5
	# Range: 0.0 ~ 40.0
	jump_feathers = 0.5

[mounts]
	#Horses, donkeys, mules and camels have feathers too: galloping and jumping tire them, rest restores them.
	#Their feathers show above yours while you ride. An exhausted mount slows down.
	mounts_enabled = true
	#Each mount is born with its own stamina, a hidden trait like its speed or health: somewhere
	#between these two, usually near the middle. Foals take after their parents.
	# Default: 14
	# Range: 1 ~ 1000
	mount_feathers_min = 14
	#The most feathers a mount can be born with.
	# Default: 30
	# Range: 1 ~ 1000
	mount_feathers_max = 30
	#Feathers a mount regenerates per second.
	# Default: 0.5
	# Range: 0.0 ~ 40.0
	mount_regen_feathers_per_second = 0.5
	#Feathers per second while galloping (ridden faster than mount_gallop_speed). Animals tire slowly:
	#at 0.1, a 20-feather horse gallops for over three minutes.
	# Default: 0.1
	# Range: 0.0 ~ 40.0
	mount_gallop_feathers_per_second = 0.1
	#Horizontal speed, in blocks per tick, from which a ridden mount counts as galloping. A walk is about 0.1.
	# Default: 0.25
	# Range: 0.0 ~ 10.0
	mount_gallop_speed = 0.25
	#Feathers for a fully charged jump (or a camel's dash); weaker jumps cost less.
	# Default: 0.5
	# Range: 0.0 ~ 40.0
	mount_jump_feathers = 0.5
	#How much an exhausted mount slows down: 0.5 = half speed until it recovers.
	# Default: 0.5
	# Range: 0.0 ~ 0.95
	mount_exhausted_slowdown = 0.5

[debugging]
	#Shows a debug overlay with the feathers state and what spent them, and logs spends.
	debug_mode = false
```

## Default `FeathersOfFatigue-Compat.toml`

```toml
#Droplets of Thirst (the continuation of Thirst Was Taken): thirst slows regeneration, being quenched speeds it up.
#Ignored for players whose thirst is off.
[droplets_of_thirst]
	#Use Droplets of Thirst when it is installed.
	enabled = true
	#Feathers per second lost per missing thirst point (20 points = full).
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_reduction_per_thirst_point = 0.02
	#Feathers per second gained per point of quenched (thirst saturation).
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_bonus_per_quench_point = 0.02
	#Extra thirst points each regenerated feather costs.
	#Droplets of Thirst already drains thirst with the food spent on regeneration, unless its mode is OWN.
	# Default: 0.0
	# Range: 0.0 ~ 20.0
	thirst_per_regenerated_feather = 0.0

#Serene Seasons: winter outdoors is cold, a summer day in the sun is hot.
[serene_seasons]
	#Use Serene Seasons when it is installed.
	enabled = true
	#Being outdoors in winter applies Cold.
	winter_cold = true
	#Only in biomes cooler than this (deserts and jungles stay warm). Plains are 0.8.
	# Default: 1.0
	# Range: -2.0 ~ 4.0
	winter_cold_below_temperature = 1.0
	#A summer day under the sun applies Heat in warm biomes.
	summer_heat = true
	#Biome temperature from which summer sun applies Heat (lower than the normal hot_temperature).
	# Default: 0.8
	# Range: -2.0 ~ 4.0
	summer_heat_from_temperature = 0.8
```

Older versions support more mods, each with a section: Tough As Nails on 26.2 and older, and Cold Sweat, Thirst Was Taken and Legendary Survival Overhaul on 1.21.1 and older (see [Minecraft Versions](Minecraft-Versions)). The Cold Sweat and Thirst Was Taken sections come before `[droplets_of_thirst]`. The other two come before `[serene_seasons]`. On those versions, Feathers of Fatigue ignores `[serene_seasons]` while a body-temperature mod is in charge:

```toml
#Cold Sweat: body temperature decides Cold, Heat and Fatigue instead of biomes.
[cold_sweat]
	#Use Cold Sweat when it is installed.
	enabled = true
	#Low body temperature applies Cold.
	cold_from_body_temperature = true
	#High body temperature applies Heat, and severe heat Fatigue.
	heat_from_body_temperature = true
	#Body temperature at or below which Cold applies. Body temperature spans -150 to 150;
	#Cold Sweat's own freezing starts around -45.
	# Default: -50
	# Range: -150 ~ 150
	cold_threshold = -50
	#Body temperature at or above which Heat applies. Overheating starts around 45.
	# Default: 50
	# Range: -150 ~ 150
	hot_threshold = 50
	#Body temperature at or above which the heat is severe and Fatigue applies too.
	# Default: 100
	# Range: -150 ~ 150
	severe_hot_threshold = 100

#Thirst Was Taken: thirst slows regeneration, being quenched speeds it up.
[thirst_was_taken]
	#Use Thirst Was Taken when it is installed.
	enabled = true
	#Feathers per second lost per missing thirst point (20 points = full).
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_reduction_per_thirst_point = 0.02
	#Feathers per second gained per point of quench (thirst saturation).
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_bonus_per_quench_point = 0.02
	#Thirst points each regenerated feather costs. 0 = regenerating costs no thirst.
	# Default: 0.0
	# Range: 0.0 ~ 20.0
	thirst_per_regenerated_feather = 0.0

#Tough As Nails: its temperature drives Cold, Heat and Fatigue; its thirst drives regeneration.
[tough_as_nails]
	#Use Tough As Nails when it is installed.
	enabled = true
	#Use its temperature (when its temperature is on).
	temperature = true
	#COLD applies the Cold effect; off: only ICY does.
	cold_level_applies_cold = true
	#WARM applies Heat too; off: only HOT does.
	warm_level_applies_heat = false
	#Hyperthermia progress (0 to 1) at which the heat is severe and Fatigue applies.
	# Default: 0.5
	# Range: 0.0 ~ 1.0
	severe_hyperthermia = 0.5
	#Use its thirst (when its thirst is on).
	thirst = true
	#Feathers per second lost per missing thirst point (20 = full).
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_reduction_per_thirst_point = 0.02
	#Feathers per second gained per point of hydration.
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_bonus_per_hydration_point = 0.02
	#Thirst exhaustion per regenerated feather (4.0 = one thirst point). 0 = free.
	# Default: 0.0
	# Range: 0.0 ~ 40.0
	thirst_exhaustion_per_regenerated_feather = 0.0

#Legendary Survival Overhaul: body temperature drives Cold, Heat and Fatigue; hydration drives regeneration.
[legendary_survival_overhaul]
	#Use Legendary Survival Overhaul when it is installed.
	enabled = true
	#Use its body temperature.
	temperature = true
	#Use its hydration.
	thirst = true
	#Feathers per second lost per missing hydration point (20 = full).
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_reduction_per_thirst_point = 0.02
	#Feathers per second gained per point of hydration saturation.
	# Default: 0.02
	# Range: 0.0 ~ 20.0
	regen_bonus_per_saturation_point = 0.02
	#Thirst exhaustion per regenerated feather. 0 = free.
	# Default: 0.0
	# Range: 0.0 ~ 40.0
	thirst_exhaustion_per_regenerated_feather = 0.0
```

## Default `FeathersOfFatigue-Client.toml`

```toml
[hud]
	#Fade the feathers out while they are full.
	fade_when_full = false
	#Ticks full before the feathers fade.
	# Default: 60
	# Range: 0 ~ 1200
	fade_cooldown_ticks = 60
	#Ticks the fade-in takes.
	# Default: 40
	# Range: 1 ~ 1200
	fade_in_ticks = 40
	#Ticks the fade-out takes.
	# Default: 40
	# Range: 1 ~ 1200
	fade_out_ticks = 40
	#Flash the feathers when one regenerates.
	regen_flash = false
	#Stack the feathers with the other bars on the right (food, air, thirst...).
	#Off: always draw them right above the food bar and let other bars sort themselves out.
	stack_with_right_bars = true
	#Horizontal offset of the feathers, in pixels.
	# Default: 0
	# Range: -1000 ~ 1000
	x_offset = 0
	#Vertical offset of the feathers, in pixels. Negative moves them up.
	# Default: 0
	# Range: -1000 ~ 1000
	y_offset = 0
	#Color of your feathers: GREEN, BLUE (Elenai's original) or WHITE (like a chicken's).
	#Mounts' feathers take their own color, and armor weight its armor's.
	#Allowed Values: GREEN, BLUE, WHITE
	feather_color = "GREEN"

[animations]
	#A wave runs along the feathers while Energized, like hearts under Regeneration.
	wave_when_energized = true
	#The feathers shake when few are left, like hearts at low health.
	shake_when_low = true
	#How many usable feathers count as few, for shake_when_low.
	# Default: 2
	# Range: 0 ~ 20
	low_feathers = 2
	#How the feathers move while strained: NONE, SHAKE or PULSE (brighten and dim).
	#Allowed Values: NONE, SHAKE, PULSE
	strain_animation = "PULSE"

[feedback]
	#Play a sound when the Cold effect freezes the feathers.
	cold_sound = true
	#Show armor weight in item tooltips.
	weight_in_tooltips = true
	#Show tooltip weights as feather icons (true) or as text (false).
	weight_as_icons = false
```
