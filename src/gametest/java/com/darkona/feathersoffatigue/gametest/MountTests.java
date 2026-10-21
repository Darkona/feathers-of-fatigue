package com.darkona.feathersoffatigue.gametest;

import com.darkona.feathersoffatigue.api.FeathersAPI;
import com.darkona.feathersoffatigue.api.FeathersView;
import com.darkona.feathersoffatigue.api.Stamina;
import com.darkona.feathersoffatigue.api.registry.FeathersAttributes;
import com.darkona.feathersoffatigue.api.registry.FeathersIds;
import com.darkona.feathersoffatigue.api.registry.FeathersMobEffects;
import com.darkona.feathersoffatigue.config.FeathersServerConfig;
import com.darkona.feathersoffatigue.core.FeathersAttachments;
import com.darkona.feathersoffatigue.mount.MountExertion;
import com.darkona.feathersoffatigue.mount.MountTraits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.darkona.feathersoffatigue.api.registry.FeathersIds.id;
import static com.darkona.feathersoffatigue.gametest.TestSupport.player;

/**
 * Mounts have feathers: a stamina trait of their own that foals inherit, jumps that cost by power, and an exhausted
 * mount slows down and cannot jump.
 */
@GameTestHolder(FeathersIds.MOD_ID)
@PrefixGameTestTemplate(false)
public class MountTests {

    private static Horse horse(GameTestHelper helper) {
        Horse horse = helper.spawn(EntityType.HORSE, new BlockPos(1, 2, 1));
        horse.setTamed(true);
        horse.equipSaddle(new ItemStack(Items.SADDLE), SoundSource.NEUTRAL);
        return horse;
    }

    @GameTest(template = "empty")
    public static void horsesHaveTheirOwnStamina(GameTestHelper helper) {
        Horse horse = horse(helper);
        FeathersView feathers = FeathersAPI.get(horse);
        helper.assertTrue(FeathersAPI.hasFeathers(horse), "a horse has feathers");
        int max = feathers.maxFeathers();
        helper.assertTrue(max >= 14 && max <= 30, "the rolled stamina is within the configured range: " + max);
        helper.assertValueEqual(feathers.stamina(), feathers.maxStamina(), "a new horse is full");

        MountTraits.ensureRolled(horse);
        helper.assertValueEqual(FeathersAPI.get(horse).maxFeathers(), max, "the trait is rolled only once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void foalsTakeAfterTheirParents(GameTestHelper helper) {
        Horse mare = horse(helper);
        Horse stallion = horse(helper);
        mare.getAttribute(FeathersAttributes.MAX_FEATHERS).setBaseValue(30);
        stallion.getAttribute(FeathersAttributes.MAX_FEATHERS).setBaseValue(28);
        Horse foal = helper.spawn(EntityType.HORSE, new BlockPos(1, 2, 1));

        NeoForge.EVENT_BUS.post(new BabyEntitySpawnEvent(mare, stallion, foal));
        double trait = foal.getAttribute(FeathersAttributes.MAX_FEATHERS).getBaseValue();
        // Average 29, nudged by at most a sixth of the 14-30 range (about 2.7).
        helper.assertTrue(trait >= 26 && trait <= 30, "the foal takes after strong parents: " + trait);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void jumpsCostByPower(GameTestHelper helper) {
        Horse horse = horse(helper);
        // Pass the rider directly because a fake player outside the level cannot mount the horse.
        int before = FeathersAPI.get(horse).stamina();
        MountExertion.chargeJump(horse, player(helper), 50);
        int expected = Stamina.ofFeathers(FeathersServerConfig.MOUNT_JUMP_FEATHERS.get() * 0.5);
        helper.assertValueEqual(before - FeathersAPI.get(horse).stamina(), expected, "a half jump costs half a full jump");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exhaustedHorsesSlowDownAndCantJump(GameTestHelper helper) {
        Horse horse = horse(helper);
        FeathersView feathers = FeathersAPI.get(horse);
        FeathersAPI.spend(horse, id("test"), feathers.maxStamina() + feathers.maxStrain());
        helper.assertTrue(FeathersAPI.get(horse).exhausted(), "spent everything, strain included");

        MountExertion.tickMount(horse);
        helper.assertTrue(MountExertion.isSlowedDown(horse), "an exhausted horse slows down");
        helper.assertFalse(horse.canJump(), "an exhausted horse can't jump");

        FeathersAPI.reset(horse);
        MountExertion.tickMount(horse);
        helper.assertFalse(MountExertion.isSlowedDown(horse), "a rested horse runs again");
        helper.succeed();
    }

    /**
     * A creature that stops being a mount (mounts turned off, a reload that drops its tag or data map entry) while
     * strained and exhausted gets back to normal: no strained effect, no slowdown, no leftover exhaustion. Only the
     * effect this mod put on it goes: one from elsewhere stays.
     */
    @GameTest(template = "empty")
    public static void exMountsAreReleased(GameTestHelper helper) {
        Horse horse = horse(helper);
        FeathersView feathers = FeathersAPI.get(horse);
        FeathersAPI.spend(horse, id("test"), feathers.maxStamina() + feathers.maxStrain());
        MountExertion.tickMount(horse);
        helper.assertTrue(horse.hasEffect(FeathersMobEffects.STRAINED), "a strained horse shows it");
        helper.assertTrue(MountExertion.isSlowedDown(horse), "an exhausted horse slows down");

        MountExertion.release(horse);
        helper.assertFalse(horse.hasEffect(FeathersMobEffects.STRAINED), "the Strained effect goes");
        helper.assertFalse(MountExertion.isSlowedDown(horse), "the slowdown goes");
        feathers = FeathersAPI.get(horse);
        helper.assertValueEqual(feathers.strain(), 0, "no strain left");
        helper.assertFalse(feathers.exhausted(), "no exhaustion left");

        horse.addEffect(new MobEffectInstance(FeathersMobEffects.STRAINED, 200));
        MountExertion.release(horse);
        helper.assertTrue(horse.hasEffect(FeathersMobEffects.STRAINED), "someone else's Strained effect stays");

        // A mount again: exhaustion slows it down as before.
        horse.removeEffect(FeathersMobEffects.STRAINED);
        FeathersAPI.spend(horse, id("test"), FeathersAPI.get(horse).availableStamina() + FeathersAPI.get(horse).maxStrain());
        MountExertion.tickMount(horse);
        helper.assertTrue(MountExertion.isSlowedDown(horse), "an exhausted mount slows down again");

        // A creature that never had feathers is left alone: no feathers appear on it.
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        MountExertion.release(cow);
        helper.assertFalse(cow.hasData(FeathersAttachments.FEATHERS), "a cow gets no feathers from the release");
        helper.succeed();
    }

    /**
     * A creature made a mount by data alone: the test datapack (src/gametest/resources) gives pigs mount stats.
     */
    @GameTest(template = "empty")
    public static void dataMakesAnyCreatureAMount(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        helper.assertTrue(FeathersAPI.hasFeathers(pig), "the data map makes a pig a mount");
        helper.assertValueEqual(FeathersAPI.get(pig).maxFeathers(), 40, "its stamina comes from its stats");
        helper.assertValueEqual(FeathersAPI.getRegenPerSecond(pig), 2.0, "and so does its regeneration");
        helper.assertFalse(FeathersAPI.hasFeathers(helper.spawn(EntityType.COW, new BlockPos(1, 2, 1))), "cows stay out");
        helper.succeed();
    }

    /**
     * The opt-out tag wins even over horses: the test datapack lists donkeys in feathers_of_fatigue:no_feathers.
     */
    @GameTest(template = "empty")
    public static void optedOutCreaturesHaveNoFeathers(GameTestHelper helper) {
        helper.assertFalse(FeathersAPI.hasFeathers(helper.spawn(EntityType.DONKEY, new BlockPos(1, 2, 1))), "donkeys opted out");
        helper.assertTrue(FeathersAPI.hasFeathers(helper.spawn(EntityType.MULE, new BlockPos(1, 2, 1))), "mules still tire");
        helper.succeed();
    }

    /**
     * The built-in data map gives stamina to other mods' mounts when those mods are installed
     * ({@code -Pcompat=naturalist,mobwrangler}). The test passes without those optional mods.
     */
    @GameTest(template = "empty")
    public static void otherModsMountsHaveFeathers(GameTestHelper helper) {
        checkSpawned(helper, "naturalist", "naturalist:ostrich");
        checkSpawned(helper, "mr_mob_wrangler", "minecraft:sniffer");
        helper.succeed();
    }

    private static void checkSpawned(GameTestHelper helper, String mod, String type) {
        if (!ModList.get().isLoaded(mod)) return;
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(type));
        if (helper.spawn(entityType, new BlockPos(1, 2, 1)) instanceof LivingEntity living) {
            helper.assertTrue(FeathersAPI.hasFeathers(living), type + " has feathers with " + mod);
        }
    }
}
